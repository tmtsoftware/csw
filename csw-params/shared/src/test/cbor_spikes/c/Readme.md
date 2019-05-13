##Installation

### Homebrew

```bash
brew tap pjk/libcbor
brew install libcbor
```

### Ubuntu 18.04 and above

```bash
sudo add-apt-repository universe
sudo apt-get install libcbor-dev
```

### Fedora & RPM friends

```bash
yum install libcbor-devel
```

For usage example, see the `sample.c` file.

##Compilation

1.Using command line:
```console
$   cd <$PATH>/libcbor-example
$   gcc main.c -lcbor -o serialized-cbor
$   ./serialized-cbor
```

2.Install Jetbrains [CLions](https://www.jetbrains.com/clion/download/#section=mac) IDE to build the project using `CMakeLists.txt`