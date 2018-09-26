package com.bcombes.tinypng

import com.tinify.*
import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.lang.Exception
import java.security.MessageDigest
import java.text.DecimalFormat

/**
 * TingPng Task
 * @author Wayne
 */
public class TinyPngTask extends DefaultTask {

    //def android
    def TinyPngExtension configuration

    TinyPngTask() {
        description = 'Tiny Resources'
        group = 'tinypng'
        outputs.upToDateWhen { false }
        //android = project.extensions.android
        configuration = project.tinyInfo
    }

    public static String formatFileSize(long fileS) {
        def df = new DecimalFormat("#.00")
        if (fileS == 0L) {
            return "0B"
        }

        if (fileS < 1024) {
            return df.format((double) fileS) + "B"
        } else if (fileS < 1048576) {
            return df.format((double) fileS / 1024) + "KB"
        } else if (fileS < 1073741824) {
            return df.format((double) fileS / 1048576) + "MB"
        } else {
            return df.format((double) fileS / 1073741824) + "GB"
        }
    }

    public static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream(){ is ->
            int read
            byte[] buffer = new byte[8192]
            while((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] md5sum = digest.digest()
        BigInteger bigInt = new BigInteger(1, md5sum)
        return bigInt.toString(16).padLeft(32, '0')
    }


    public static TinyPngInfo compress(File imgFile, Iterable<String> whiteList, Iterable<TinyPngInfo> compressedList, String projParentDirPath) {
        //def newCompressedList = new ArrayList<TinyPngInfo>()
        def tinyPngInfo
        def accountError = false
        def beforeTotalSize = 0
        def afterTotalSize = 0
        def filePath = imgFile.path
        def fileName = imgFile.name

        for (String s : whiteList) {
            if (fileName ==~/$s/) {
                println("match white list, skip it >>>>>>>>>>>>> $filePath")
                return null
            }
        }

        def relativePath = filePath.replace(projParentDirPath, "")
        for (TinyPngInfo info : compressedList) {
            if (relativePath == info.path && generateMD5(imgFile) == info.md5) {
                println("file already optimized >>>>>>>>>>>>> $filePath")
                return null
            }
        }

        println("find target pic >>>>>>>>>>>>> $filePath\n")

        def fis = new FileInputStream(imgFile)

        try {
            def beforeSize = fis.available()
            def beforeSizeStr = formatFileSize(beforeSize)

            // Use the Tinify API client
            def tSource = Tinify.fromFile("${filePath}")
            tSource.toFile("${filePath}")

            def afterSize = fis.available()
            def afterSizeStr = formatFileSize(afterSize)

            beforeTotalSize += beforeSize
            afterTotalSize += afterSize


            //Remove absolute path from TinyPng file path info
            tinyPngInfo = new TinyPngInfo(relativePath, beforeSize, afterSize, generateMD5(imgFile))

            println("beforeSize: $beforeSizeStr -> afterSize: ${afterSizeStr}")
        } catch (AccountException e) {
            println("AccountException: ${e.getMessage()}")
            accountError = true
            return
            // Verify your API key and account limit.
        } catch (ClientException e) {
            // Check your source image and request options.
            println("ClientException: ${e.getMessage()}")
        } catch (ServerException e) {
            // Temporary issue with the Tinify API.
            println("ServerException: ${e.getMessage()}")
        } catch (ConnectionException e) {
            // A network connection error occurred.
            println("ConnectionException: ${e.getMessage()}")
        } catch (IOException e) {
            // Something else went wrong, unrelated to the Tinify API.
            println("IOException: ${e.getMessage()}")
        } catch (Exception e) {
            println("Exception: ${e.toString()}")
        }
        return tinyPngInfo
    }

    public static TinyPngResult scanDirectoryForImageFiles(File directory, List<TinyPngInfo> compressedList, List<TinyPngInfo> newCompressedList, TinyPngExtension configuration,
                                                           String projParentDirPath) {
        TinyPngResult result = new TinyPngResult()
        directory.eachFileRecurse (FileType.ANY) { file ->
            if(file.isDirectory()) {
                configuration.excludeDirs.each { dirPattern ->
                    if(!file.getName().endsWith(dirPattern)) {
                        result.addResult(scanDirectoryForImageFiles(file, compressedList, newCompressedList, configuration, projParentDirPath));
                    } else {
                        print("skipping directory ${dirPattern} excluded")
                    }
                }
            } else if(file.isFile()) {
                configuration.resourcePattern.each { pattern ->
                    if(file.getName().matches(~/$pattern/)) {
                        def imgFile = file
                        TinyPngInfo info = compress(imgFile, configuration.whiteList, compressedList, projParentDirPath)
                        if (info != null) {
                            result.AddInfo(info)
                            //beforeSize += result.preSize
                            //afterSize += result.postSize
                            //error = result.error
                            newCompressedList.add(info)
                        } else {
                            print("${imgFile} returning null\n")
                        }
                    }
                }
            }
        }
        return result;
    }

    @TaskAction
    def run() {
        println(configuration.toString())

        if (!(configuration.apiKey ?: false)) {
            println("Tiny API Key not set")
            return
        }

        def apiKey = configuration.apiKey
        try {
            Tinify.setKey("${apiKey}")
            Tinify.validate()
        } catch (Exception ignored) {
            println("Tiny Validation of API key failed.")
            ignored.printStackTrace()
            return
        }

        def compressedList = new ArrayList<TinyPngInfo>()
        def compressedListFile = new File("${project.projectDir}/compressed-resource.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        }
        else {
            try {
                def list = new JsonSlurper().parse(compressedListFile, "utf-8")
                if(list instanceof ArrayList) {
                    compressedList = list
                }
                else {
                    println("compressed-resource.json is invalid, ignore")
                }
            } catch (Exception ignored) {
                println("compressed-resource.json is invalid, ignore")
            }
        }

//        def beforeSize = 0L
//        def afterSize = 0L
//        def error = false
        TinyPngResult finalResult  = null
        def newCompressedList = new ArrayList<TinyPngInfo>()
        //configuration.resourceDir.each { d ->
        def rootDir = new File("${project.projectDir}")
        def projParentDirPath = rootDir.getParentFile().getPath();
        println("project root set to ... ${rootDir}")
        if(rootDir.exists() && rootDir.isDirectory()) {
            if (!(configuration.resourcePattern ?: false)) {
                configuration.resourcePattern = [".+\\.png", ".+\\.jpg", ".+\\.jpeg"]
            }
            finalResult = scanDirectoryForImageFiles(rootDir, compressedList, newCompressedList, configuration, projParentDirPath);
        }


        if(newCompressedList) {
            for (TinyPngInfo newTinyPng : newCompressedList) {
//                def truncatedPath = .replace(projectRootDirectory.parent, "")
                def index = compressedList.path.indexOf(newTinyPng.path)
                if (index >= 0) {
                    compressedList[index] = newTinyPng
                } else {
                    compressedList.add(0, newTinyPng)
                }
            }
            def jsonOutput = new JsonOutput()
            def json = jsonOutput.toJson(compressedList)
            compressedListFile.write(jsonOutput.prettyPrint(json), "utf-8")
            println("Task finish, compress ${newCompressedList.size()} files, before total size: ${formatFileSize(finalResult.beforeSize)} after total size: ${formatFileSize(finalResult.afterSize)}")
        }
    }
}