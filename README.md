# RandomTeleporter

A Fabric mod that adds the `/rtp` command, which teleports players to random positions in the
world. It puts a small glass platform under the landing position when the ground there would not
hold the player. In dimensions with a ceiling, such as the Nether, players land below the ceiling
instead of on top of it.

## Requirements

- The Minecraft version set in [`gradle.properties`](./gradle.properties)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

## Commands

- `/rtp`
- `/rtp back`
- `/rtp back <target>`
- `/rtp <target> back`
- `/rtp <radius>`
- `/rtp <target>`
- `/rtp <radius> <target>`
- `/rtp <target> <radius>`
- `/rtp <radius> <originPos>`
- `/rtp <radius> <target> <originEntity>`
- `/rtp <radius> <target> <originPos>`
- `/rtp <target> <radius> <originEntity>`
- `/rtp <target> <radius> <originPos>`
- `/rtp <regionFromPos> <regionToPos>`
- `/rtp <regionFromPos> <regionToPos> <target>`
- `/rtp <target> <regionFromPos> <regionToPos>`
- `/rtp <regionFromEntity> <regionToEntity>`
- `/rtp <regionFromEntity> <regionToEntity> <target>`

Without a target, the command teleports the player who runs it. Without a radius or region, it
picks a position up to 28,990,000 blocks from `(0, 0)` on each axis.

`/rtp <radius> <originEntity>` does not exist, because both the target and the origin are entities,
so the command could not tell them apart. For the same reason,
`/rtp <target> <regionFromEntity> <regionToEntity>` does not exist either.

### Examples

- `/rtp` teleports you anywhere in the world.
- `/rtp back` teleports you back to where you were before your last random teleport. The mod
  forgets these positions when the server stops.
- `/rtp back Steve` and `/rtp Steve back` teleport `Steve` back to where they were before their last
  random teleport.
- `/rtp 1000` teleports you up to `1000` blocks from `(0, 0)`.
- `/rtp Steve` teleports `Steve` anywhere in the world.
- `/rtp 1000 Steve` and `/rtp Steve 1000` teleport `Steve` up to `1000` blocks from `(0, 0)`.
- `/rtp 1000 10000 10000` teleports you up to `1000` blocks from `(10000, 10000)`.
- `/rtp 1000 Steve Alex` and `/rtp Steve 1000 Alex` teleport `Steve` up to `1000` blocks from
  `Alex`.
- `/rtp 1000 Steve 10000 10000` and `/rtp Steve 1000 10000 10000` teleport `Steve` up to `1000`
  blocks from `(10000, 10000)`.
- `/rtp 10000 10000 20000 20000` teleports you into the rectangle between the opposite corners
  `(10000, 10000)` and `(20000, 20000)`.
- `/rtp Steve 10000 10000 20000 20000` and `/rtp 10000 10000 20000 20000 Steve` teleport `Steve`
  into the same rectangle.
- `/rtp Steve Alex` teleports you into the rectangle between the positions of `Steve` and `Alex`.
- `/rtp Steve Alex Notch` teleports `Notch` into the rectangle between the positions of `Steve`
  and `Alex`.

## Permissions

Like the vanilla `/tp` command, `/rtp` requires permission level 2. In singleplayer, that means
cheats must be on.

## Installation

The mod runs wherever the server runs:

- In singleplayer, and when you open a world to LAN, install it on the client that hosts the world.
- On a dedicated server, install it on the server.

Players who join without the mod can still use the command, but they see its messages in English.
