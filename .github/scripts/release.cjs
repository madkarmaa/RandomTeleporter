const { execFileSync } = require("node:child_process");
const { openAsBlob, readFileSync, writeFileSync } = require("node:fs");
const { basename } = require("node:path");

const MODRINTH_API = "https://api.modrinth.com/v2";

// Modrinth names dependencies by project ID, not by slug.
const MODRINTH_DEPENDENCIES = [
    { project_id: "P7dR8mSH", dependency_type: "required" }, // Fabric API
    { project_id: "Ha28R6CL", dependency_type: "required" }, // Fabric Language Kotlin
];

const readProperty = (cwd, name) => {
    const content = readFileSync(`${cwd}/gradle.properties`, "utf8");
    const value = content.match(new RegExp(`^${name}=(.+)$`, "m"))?.[1];
    if (!value) throw new Error(`gradle.properties sets no ${name}`);
    return value;
};

// The mod ID is also the slug of the Modrinth project and the start of the jar's name.
const readModId = (cwd) =>
    JSON.parse(readFileSync(`${cwd}/src/main/resources/fabric.mod.json`, "utf8")).id;

const modrinth = async (path, env, init = {}) => {
    const response = await fetch(`${MODRINTH_API}${path}`, {
        ...init,
        headers: { Authorization: env.MODRINTH_TOKEN, "User-Agent": "RandomTeleporter release" },
    });
    if (!response.ok) {
        throw new Error(`Modrinth refused ${path}: ${response.status} ${await response.text()}`);
    }

    return response.json();
};

// Finding the project before the release starts keeps a missing token or project from leaving a
// release on GitHub that never reached Modrinth.
exports.verifyConditions = async (_, { cwd, env }) => {
    if (!env.MODRINTH_TOKEN) throw new Error("MODRINTH_TOKEN is not set");

    await modrinth(`/project/${readModId(cwd)}`, env);
};

// The release version goes into gradle.properties, so the jar carries it.
exports.prepare = (_, { nextRelease, cwd, env }) => {
    const path = `${cwd}/gradle.properties`;
    const content = readFileSync(path, "utf8");
    const pattern = /^version=.*$/m;
    if (!pattern.test(content)) throw new Error(`No version found in ${path}`);

    writeFileSync(path, content.replace(pattern, `version=${nextRelease.version}`));
    execFileSync("./gradlew", ["build"], { cwd, env, stdio: "inherit" });
};

exports.publish = async (_, { nextRelease, cwd, env, logger }) => {
    const modId = readModId(cwd);
    const minecraftVersion = readProperty(cwd, "minecraft_version");
    const versionName = `${nextRelease.version}+${minecraftVersion}`;
    const jarPath = `${cwd}/build/libs/${modId}-${versionName}.jar`;
    const project = await modrinth(`/project/${modId}`, env);

    const form = new FormData();
    form.append(
        "data",
        JSON.stringify({
            project_id: project.id,
            name: versionName,
            version_number: versionName,
            changelog: nextRelease.notes,
            dependencies: MODRINTH_DEPENDENCIES,
            game_versions: [minecraftVersion],
            loaders: ["fabric"],
            version_type: "release",
            featured: true,
            file_parts: ["jar"],
            primary_file: "jar",
        }),
    );
    form.append("jar", await openAsBlob(jarPath), basename(jarPath));

    const version = await modrinth("/version", env, { method: "POST", body: form });
    const url = `https://modrinth.com/mod/${modId}/version/${version.id}`;
    logger.log(`Published ${versionName} to Modrinth: ${url}`);
    return { name: "Modrinth", url };
};
