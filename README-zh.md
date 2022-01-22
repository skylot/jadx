<img src="https://raw.githubusercontent.com/skylot/jadx/master/jadx-gui/src/main/resources/logos/jadx-logo.png" width="64" align="left" />

## JADX

[![构建状态](https://github.com/skylot/jadx/workflows/Build/badge.svg)](https://github.com/skylot/jadx/actions?query=workflow%3ABuild)

[![Alerts from lgtm.com](https://img.shields.io/lgtm/alerts/g/skylot/jadx.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/skylot/jadx/alerts/)

[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

[![许可证书](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

**jadx** - Dex 转 Java 的解析器

用于从`Android Dex`和 `Apk文件` 生成Java源代码的命令行和GUI工具

:exclamation: :exclamation: :exclamation: 请注意，在大多数情况下，`Jadx`不能反编译所有100%的代码，因此会发生错误。查看[故障排除指南](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A#decompilation-issues) 的变通办法

**主要功能**

- 从 `apk , dex , aar , aab , zip` 文件中反编译 `Dalvik字节码` 为 `java`
- 从 `resources.arsc` 解码  `AndroidManifest.xml`  和其他资源文件
- 支持反混淆

**jadx-gui 特色 :**

- 用高亮显示的语法查看反编译的代码
- 跳转到声明处
- 寻找调用
- 全文搜索
- smali 调试器(感谢[@LBJ-the-GOAT](https://github.com/LBJ-the-GOAT)的贡献)，通过[wiki页面](https://github.com/skylot/jadx/wiki/Smali-debugger)了解它的设置和使用教程

在这里查看这些功能的运行情况 :  [jadx-gui features overview](https://github.com/skylot/jadx/wiki/jadx-gui-features-overview)
<img src="https://user-images.githubusercontent.com/118523/142730720-839f017e-38db-423e-b53f-39f5f0a0316f.png" width="700"/>

### 下载

- 稳定版 [github: ![最新稳定版](README-zh.assets/jadx.svg)](https://github.com/skylot/jadx/releases/latest) 
- 最新版 [测试版](https://nightly.link/skylot/jadx/workflows/build/master)

下载完 `zip 文件`后，进入 `bin` 目录并运行:

- `jadx` - 命令行方式工作
- `jadx-gui` - 图形方式工作

在Windows上双击运行 `.bat` 文件
注意: 确保已安装64位Java 11及以上版本。
对于windows，你可以从 [oracle.com](https://www.oracle.com/java/technologies/downloads/#jdk17-windows) 下载(选择x64安装程序)。

### 安装

1. Arch linux

   ```bash
       sudo pacman -S jadx
   ```

2. macOS

   ```bash
       brew install jadx
   ```

### 使用 `jadx` 作为lib库调用

可在maven中央存储库([列表](https://search.maven.org/search?q=jadx))  获取 jadx `1.3.1`  的jadx插件。

#### 导入项目

1. 添加主 `jadx-core` 依赖 (`io.github.skylot:jadx-core`)
2. 添加 `google()`存储库来加载 `aapt` 依赖(将在未来修复)
3. 添加一个或几个输入插件:

- `jadx-dex-input` - 允许读取dex文件和所有包装器(apk等)

- `jadx-java-input` - 支持Java字节码加载

- `jadx-java-convert` - 通过使用dx/d8工具的转换来支持Java字节码(与 `jadx-java-input` 冲突)

- `jadx-smali-input` - [Smali](https://github.com/JesusFreke/smali) 输入的支持

- `jadx-raung-input` - [Raung](https://github.com/skylot/raung) 输入的支持
4. (可选)jadx使用 `slf4j-api` ([manual](http://www.slf4j.org/manual.html)) 后, 您需要添加并配置实现库 `ch.qos.logback:logback-classic` ([manual](http://logback.qos.ch/manual/index.html))



#### 例子

从输入转储简单代码的完整示例：

```
import java.io.File;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
public class App {
    public static void main(String[] args) {
        JadxArgs jadxArgs = new JadxArgs();
        jadxArgs.setInputFile(new File("classes.dex"));
        jadxArgs.setOutDir(new File("output"));
        try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
            jadx.load();
            jadx.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

取代 `jadx.save()` ，你可以遍历类来访问必要的信息：

```
for (JavaClass cls : jadx.getClasses()) {
    for (JavaMethod mth : cls.getMethods()) {
        System.out.println(mth.getName());
    }
}
```



#### 可能的优化

1. 如果不需要代码属性，可以切换到简单的代码编写器：

```
jadxArgs.setCodeWriterProvider(SimpleCodeWriter::new);
```

​                     

1. 使用简单的dump来减少内存的占用(类信息只访问一次)：

```
jadxArgs.setCodeCache(new NoOpCodeCache());
```





### 从源代码构建

必须安装`JDK 8`或更高版本 :

```
git clone https://github.com/skylot/jadx.git
cd jadx
./gradlew dist
```

(在 Windows 中 , 使用 `gradlew.bat` 替代 `./gradlew`)

运行`jadx的脚本`将被放置在 `build/jadx/bin` 中

也打包到 `build/jadx-<version>.zip` 

### 用法

```
jadx[-gui] [options] <input files> (.apk, .dex, .jar, .class, .smali, .zip, .aar, .arsc, .aab)
options:
  -d, --output-dir                    - 输出目录
  -ds, --output-dir-src               - output directory for sources
  -dr, --output-dir-res               - output directory for resources
  -r, --no-res                        - 不解码 resources
  -s, --no-src                        - do not decompile source code
  --single-class                      - decompile a single class
  --output-format                     - can be 'java' or 'json', default: java
  -e, --export-gradle                 - save as android gradle project
  -j, --threads-count                 - processing threads count, default: 4
  --show-bad-code                     - 显示不一致的代码(不正确的反编译)
  --no-imports                        - 禁用导入，始终写入整个包名
  --no-debug-info                     - 禁用调试信息
  --add-debug-lines                   - 如果可用，添加带有调试行号的注释
  --no-inline-anonymous               - 禁用内联匿名类 anonymous classes
  --no-inline-methods                 - 禁用内联方法
  --no-replace-consts                 - 不要用匹配的常量字段替换常量值
  --escape-unicode                    - 转义字符串中的非拉丁字符 ( \u 开头的字符串)
  --respect-bytecode-access-modifiers - 不要更改原始的访问修饰符
  --deobf                             - 激活反混淆 activate deobfuscation
  --deobf-min                         - min length of name, renamed if shorter, default: 3
  --deobf-max                         - max length of name, renamed if longer, default: 64
  --deobf-cfg-file                    - 反混淆映射文件，默认值:与输入文件相同的目录和名称。jobf的扩展
  --deobf-rewrite-cfg                 - 强制忽略和覆盖反混淆映射文件
  --deobf-use-sourcename              - 使用源文件名作为类名别名
  --deobf-parse-kotlin-metadata       - 将kotlin元数据解析为类和包名
  --rename-flags                      - 修复操作 (comma-separated list of):
                                         'case' - 修复区分大小写的问题(according to --fs-case-sensitive option),
                                         'valid' - 重命名Java标识符以使其有效,
                                         'printable' - 从标识符中删除不可打印的字符，
                                        or single 'none' - 禁用所有重命名
                                        or single 'all' -  使用所有选项(default)
  --fs-case-sensitive                 - 将文件系统区分大小写，默认为false
  --cfg                               - 保存方法控制流图到点文件
  --raw-cfg                           - 保存方法控制流程图(使用原始指令)
  -f, --fallback                      - 使简单的转换 (using goto instead of 'if', 'for', etc)
  --use-dx                            - 使用dx/d8转换Java字节码
  --comments-level                    - 设置代码注释级别， values: none, user_only, error, warn, info, debug, default: info
  --log-level                         - 设置 log 等级, values: quiet, progress, error, warn, info, debug, default: progress
  -v, --verbose                       - 详细输出 (set --log-level to DEBUG)
  -q, --quiet                         - 关闭输出 (set --log-level to QUIET)
  --version                           - 打印jadx版本
  -h, --help                          - 打印此帮助
Examples:
  jadx -d out classes.dex
  jadx --rename-flags "none" classes.dex
  jadx --rename-flags "valid, printable" classes.dex
  jadx --log-level ERROR app.apk
```

这些选项也适用于从命令行运行的jadx-gui和从首选项对话框重写选项

### 故障排除

请检查 wiki 页 [Troubleshooting Q&A](https://github.com/skylot/jadx/wiki/Troubleshooting-Q&A)

### 贡献

为了支持这个项目，你可以 :

  - 发布对你来说重要的新功能/优化的想法
  - 请在提交编译问题前阅读: [Open issue](CONTRIBUTING.md#Open-Issue)
  - 打开拉取请求时，请遵循以下规则: [Pull Request Process](CONTRIBUTING.md#Pull-Request-Process)

---------------------------------------

*Licensed under the Apache 2.0 License*
