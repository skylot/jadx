### Supported publish locations for Jadx plugins

---

#### GitHub release artifact

Pattern: `github:<owner>:<repo>[:<version>][:<artifact name prefix>]`

Examples: `github:skylot:jadx`, `github:skylot:jadx:sample-plugin` or `github:skylot:jadx:0.1.0`

`<version>` - exact version to install (optional), should be equal to release name

Artifact name pattern: `<artifact name prefix>[-<release-version-name>].jar`.

Default value for `<artifact name prefix>` is a repo name, `-<release-version-name>` is optional.

---

#### Local file

Install local jar file.

Pattern: `file:<path to file>.jar`

Example: `file:/home/user/plugin.jar`

As alternative to install, plugin jars can be copied to `plugins/dropins` folder.
