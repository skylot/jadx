## JADX scripting support

:exclamation: Work still in progress! Script API is not stable!

### Examples

Check script examples in [`examples/scripts/`](https://github.com/skylot/jadx/tree/master/jadx-plugins/jadx-script/examples/scripts)(start with [`hello`](https://github.com/skylot/jadx/blob/master/jadx-plugins/jadx-script/examples/scripts/hello.jadx.kts))

### Script usage

#### In jadx-cli

Just add script file as input

#### In jadx-gui

1. Add script file to the project (using `Add files` or `New script` by right-click menu on `Inputs/Scripts`)
2. Script will appear in `Inputs/Scripts` section
3. After script change, you can run it using `Run` button in script editor toolbar or reload whole project (`Reload` button in toolbar or `F5`).
   Also, you can enable `Live reload` option in `File` menu to reload project automatically on scripts change

### Script development

Jadx-gui for now don't support ~~autocompletion~~, ~~errors highlighting~~, code navigation and docs,
so the best approach for script editing is to open jadx project in IntelliJ IDEA and write your script in `examples/scripts/` folder.
Also, this allows to debug your scripts: for that you need to create run configuration for jadx-cli or jadx-gui
add breakpoints and next run it in debug mode (jadx-gui is preferred because of faster script reload).

Script logs and compilation errors will appear in `Log viewer` (try filter for show only script related logs)
