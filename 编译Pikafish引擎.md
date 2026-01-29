# 编译 Pikafish 引擎 - Android ARM 版本

## 为什么需要编译？

Pikafish 是 C++ 程序，手机使用 ARM 处理器，需要编译成 ARM 版本才能在 Android 上运行。

## 方法 1：使用 Android NDK 编译（推荐）

### 准备工作

1. **安装 Android NDK**
   - 打开 Android Studio
   - Tools → SDK Manager
   - SDK Tools 标签页
   - 勾选 "NDK (Side by side)"
   - 点击 Apply 安装

2. **下载 Pikafish 源码**
   ```bash
   git clone https://github.com/official-pikafish/Pikafish.git
   cd Pikafish/src
   ```

### 编译步骤

#### Windows 系统

1. **设置 NDK 环境变量**
   ```cmd
   set NDK_PATH=C:\Users\你的用户名\AppData\Local\Android\Sdk\ndk\26.1.10909125
   set PATH=%NDK_PATH%\toolchains\llvm\prebuilt\windows-x86_64\bin;%PATH%
   ```

2. **编译 ARM64 版本**
   ```cmd
   cd Pikafish\src
   make clean
   make build ARCH=armv8 COMP=ndk
   ```

3. **编译 ARMv7 版本**
   ```cmd
   make clean
   make build ARCH=armv7 COMP=ndk
   ```

#### Linux/Mac 系统

1. **设置 NDK 路径**
   ```bash
   export NDK_PATH=~/Android/Sdk/ndk/26.1.10909125
   export PATH=$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
   ```

2. **编译**
   ```bash
   cd Pikafish/src
   make clean
   make build ARCH=armv8 COMP=ndk
   make clean
   make build ARCH=armv7 COMP=ndk
   ```

### 复制文件到项目

编译完成后，将生成的文件复制到 Android 项目：

```
Pikafish/src/pikafish → XiangqiAssistant_Android/app/src/main/jniLibs/arm64-v8a/libpikafish.so
Pikafish/src/pikafish → XiangqiAssistant_Android/app/src/main/jniLibs/armeabi-v7a/libpikafish.so
```

**注意**：需要重命名为 `.so` 文件并放在对应的架构文件夹中。

## 方法 2：使用预编译版本（简单）

### 从 GitHub Releases 下载

1. 访问：https://github.com/official-pikafish/Pikafish/releases
2. 找到最新版本
3. 下载 Android 版本（如果有）：
   - `pikafish-android-arm64-v8a`
   - `pikafish-android-armeabi-v7a`

### 如果没有 Android 版本

可以使用 Termux 在手机上编译：

1. **安装 Termux**（从 F-Droid 下载）
2. **在 Termux 中执行**：
   ```bash
   pkg install git clang make
   git clone https://github.com/official-pikafish/Pikafish.git
   cd Pikafish/src
   make build ARCH=armv8
   ```

3. **复制编译好的文件**到电脑

## 方法 3：修改 Makefile 支持 NDK

创建一个专门的 Android 编译配置：

### 修改 Pikafish/src/Makefile

在 Makefile 中添加 Android NDK 支持：

```makefile
# Android NDK 配置
ifeq ($(COMP),ndk)
    ifeq ($(ARCH),armv8)
        CXX = aarch64-linux-android21-clang++
        CXXFLAGS += -march=armv8-a
    endif
    ifeq ($(ARCH),armv7)
        CXX = armv7a-linux-androideabi21-clang++
        CXXFLAGS += -march=armv7-a -mfloat-abi=softfp -mfpu=neon
    endif
    LDFLAGS += -static-libstdc++
endif
```

然后编译：
```bash
make build ARCH=armv8 COMP=ndk
```

## 集成到 Android 项目

### 1. 创建 JNI 接口

创建文件：`app/src/main/cpp/pikafish_jni.cpp`

```cpp
#include <jni.h>
#include <string>
#include <unistd.h>

extern "C" {

// 启动 Pikafish 引擎
JNIEXPORT jlong JNICALL
Java_com_xiangqi_assistant_engine_PikafishEngine_nativeStart(
    JNIEnv* env, jobject thiz, jstring enginePath) {
    
    const char* path = env->GetStringUTFChars(enginePath, nullptr);
    
    // 创建管道
    int stdin_pipe[2];
    int stdout_pipe[2];
    pipe(stdin_pipe);
    pipe(stdout_pipe);
    
    pid_t pid = fork();
    if (pid == 0) {
        // 子进程：运行引擎
        dup2(stdin_pipe[0], STDIN_FILENO);
        dup2(stdout_pipe[1], STDOUT_FILENO);
        close(stdin_pipe[1]);
        close(stdout_pipe[0]);
        
        execl(path, "pikafish", nullptr);
        exit(1);
    }
    
    // 父进程
    close(stdin_pipe[0]);
    close(stdout_pipe[1]);
    
    env->ReleaseStringUTFChars(enginePath, path);
    
    // 返回文件描述符
    return (jlong)((stdin_pipe[1] << 32) | stdout_pipe[0]);
}

// 发送命令
JNIEXPORT void JNICALL
Java_com_xiangqi_assistant_engine_PikafishEngine_nativeSendCommand(
    JNIEnv* env, jobject thiz, jlong handle, jstring command) {
    
    int stdin_fd = (int)(handle >> 32);
    const char* cmd = env->GetStringUTFChars(command, nullptr);
    
    write(stdin_fd, cmd, strlen(cmd));
    write(stdin_fd, "\n", 1);
    
    env->ReleaseStringUTFChars(command, cmd);
}

// 读取输出
JNIEXPORT jstring JNICALL
Java_com_xiangqi_assistant_engine_PikafishEngine_nativeReadLine(
    JNIEnv* env, jobject thiz, jlong handle) {
    
    int stdout_fd = (int)(handle & 0xFFFFFFFF);
    char buffer[4096];
    
    int i = 0;
    while (i < sizeof(buffer) - 1) {
        char c;
        if (read(stdout_fd, &c, 1) <= 0) break;
        if (c == '\n') break;
        buffer[i++] = c;
    }
    buffer[i] = '\0';
    
    return env->NewStringUTF(buffer);
}

}
```

### 2. 修改 build.gradle

在 `app/build.gradle` 中添加：

```gradle
android {
    ...
    
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['src/main/jniLibs']
        }
    }
}
```

### 3. 创建 CMakeLists.txt

创建文件：`app/src/main/cpp/CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.18.1)
project("pikafish_jni")

add_library(pikafish_jni SHARED pikafish_jni.cpp)

find_library(log-lib log)
target_link_libraries(pikafish_jni ${log-lib})
```

## 简化方案：直接打包引擎

### 最简单的方法

1. **下载预编译的 Pikafish**（从其他来源）
2. **放入 assets 文件夹**：
   ```
   app/src/main/assets/pikafish-arm64
   app/src/main/assets/pikafish-armv7
   ```

3. **首次运行时复制到可执行目录**：
   ```kotlin
   private fun copyEngineFromAssets() {
       val assetName = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
           "pikafish-arm64"
       } else {
           "pikafish-armv7"
       }
       
       val outputFile = File(filesDir, "pikafish")
       assets.open(assetName).use { input ->
           outputFile.outputStream().use { output ->
               input.copyTo(output)
           }
       }
       
       // 设置可执行权限
       outputFile.setExecutable(true)
   }
   ```

## 总结

**推荐流程**：

1. ✅ 使用 Android NDK 编译 Pikafish（方法 1）
2. ✅ 将编译好的文件放入 `jniLibs` 文件夹
3. ✅ 或者放入 `assets` 文件夹，运行时复制
4. ✅ 通过 JNI 或 Process 调用引擎

**如果编译困难**：
- 可以先用简化的象棋算法代替
- 或者使用在线 API
- 等有人编译好 Android 版本后再集成

---

**需要帮助？** 如果编译遇到问题，可以：
1. 查看 Pikafish 官方文档
2. 在 GitHub Issues 中寻求帮助
3. 使用 Termux 在手机上直接编译
