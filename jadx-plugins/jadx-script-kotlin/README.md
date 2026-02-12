## JADX scripting support

### Examples

Check script examples in [`examples/`](https://github.com/skylot/jadx/tree/master/jadx-plugins/jadx-script-kotlin/examples/)(start with [`hello`](https://github.com/skylot/jadx/blob/master/jadx-plugins/jadx-script-kotlin/examples/hello.jadx.kts))

### Script usage

#### In jadx-cli

Just add script file as input

#### In jadx-gui

1. Add script file to the project (using `Add files` or `New script` by right-click menu on `Inputs/Scripts`)
2. Script will appear in `Inputs/Scripts` section
3. After script change, you can run it using `Run` button in script editor toolbar or reload whole project (`Reload` button in toolbar or `F5`).
   Also, you can enable `Live reload` option in `File` menu to reload project automatically on scripts change
