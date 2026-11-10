# Development standards

- Keep implementation details out of README.md.

- Keep the code simple and easy to read. Avoid complex structures and unnecessary abstractions.

- Use clear and descriptive names for variables, functions, and classes. Avoid abbreviations and single-letter names.

- Write comments only where they are needed. A comment explains why the code does something, not what it does.

- Prefer early guards in the code to reduce indentation.

- Run `./gradlew spotlessApply` as the last step before considering a task done.

- Run Entanglement against a baseline regularly and refactor based on its results. Every edited file must stay green, with a maintainability index of at least 20.

# Minecraft standards

- Target the Minecraft version in `gradle.properties`, which has unobfuscated Mojang names. Verify game and Fabric API signatures against the actual jars instead of relying on memory.

- Give every player-facing message a translation key in `en_us.json` and the same English text as its fallback, since players without the mod only see the fallback.

# Git standards

- Use Conventional Commits. The release workflow derives the next version from them.

- Keep commit messages short and precise.

- Make each commit one logical change. Do not bundle unrelated changes into one commit.
