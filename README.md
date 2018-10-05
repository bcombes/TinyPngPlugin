[![Build Status](https://semaphoreapp.com/api/v1/projects/d4cca506-99be-44d2-b19e-176f36ec8cf1/128505/shields_badge.svg)](https://github.com/waynell/TinyPngPlugin) [ ![Download](https://api.bintray.com/packages/waynell/maven/TinyPngPlugin/images/download.svg?version=1.3) ](https://plugins.gradle.org/plugin/com.bcombes.tinypng)

### TinyPngPlugin
`TinyPngPlugin` is a Gradle plugin for [TinyPng](https://tinypng.com/), which can optimize images found in any gradle software project.

### Get Tiny API key
Before using this plugin, you will require a Tiny API Key. Goto [Tiny Developers Page](https://tinypng.com/developers), input your email and name to get the key.

*Notice: The first 500 compressions each month are free. You will only be billed if you compress more than 500 images.*

### Getting Started
Add `TinyPngPlugin` as a dependency in your main build.gradle in the buildscript section of your configuration root of your project:

    dependencies {
        classpath 'gradle.plugin.com.bcombes.tinypng:TinyPngPlugin:1.3'
    }

Then you need to apply the plugin and configure your `tinyinfo` by adding the following lines to your `app/build.gradle`:

 	apply plugin: 'com.bcombes.tinypng'
 	
     tinyInfo {
        resourcePattern = [
                //apply your image regex patterns here n.b: only png and jpg formats supported
                ".+\\.png",
                ".+\\.jpeg",
                ".+\\.jpg"
        ]
        excludeDirs = [
                //here you can include any project folders you would like to skip
                "build",
                "gradle",
                "out",
                ".git"
        ]
        whiteList = [
                // your white list, support Regular Expressions
        ]
        apiKey = 'your tiny API key'
    }

In `Android Studio` and `IDEA IntelliJ`, you can find the tinyPng task option in `tinypng` group. Or alternatively, you can run `./gradlew tinyPng` from your terminal.

`TinyPngPlugin` has a `compressed-resource.json` file to record the compression results, on subsequent runs, `TinyPngPlugin` will skip files that have already been compressed.

### Thanks
[TinyPngPlugin](https://github.com/waynell/TinyPngPlugin)

### Licence
MIT License

Copyright (c) 2018 Bayo Puddicombe

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
