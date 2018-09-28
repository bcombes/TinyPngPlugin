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
import org.gradle.api.logging.Logger

/**
 * TingPng Task
 * @author Wayne
 */
public class TinyPngTask extends DefaultTask {

    //def android
    def TinyPngExtension configuration
    def Logger logger

    TinyPngTask() {
        description = 'Tiny Resources'
        group = 'tinypng'
        outputs.upToDateWhen { false }
        //android = project.extensions.android
        configuration = project.tinyInfo
        logger = project.logger
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


    public static TinyPngInfo compress(File imgFile, Iterable<String> whiteList, Iterable<TinyPngInfo> compressedList, String projParentDirPath, Logger logger) {
        def tinyPngInfo
        def accountError = false
        //def beforeTotalSize = 0
        //def afterTotalSize = 0
        def filePath = imgFile.path
        def fileName = imgFile.name

        for (String s : whiteList) {
            if (fileName ==~/$s/) {
                logger.info("match white list, skip it >>>>>>>>>>>>> $filePath")
                return null
            }
        }

        def relativePath = filePath.replace(projParentDirPath, "")
        for (TinyPngInfo info : compressedList) {
            if (relativePath == info.path && generateMD5(imgFile) == info.md5) {
                logger.info("file already optimized >>>>>>>>>>>>> $filePath")
                return null
            }
        }

        logger.debug("find target pic >>>>>>>>>>>>> $filePath\n")

        def fis = new FileInputStream(imgFile)

        try {
            def beforeSize = fis.available()
            def beforeSizeStr = formatFileSize(beforeSize)

            // Use the Tinify API client
            def tSource = Tinify.fromFile("${filePath}")
            tSource.toFile("${filePath}")

            def afterSize = fis.available()
            def afterSizeStr = formatFileSize(afterSize)

//            beforeTotalSize += beforeSize
//            afterTotalSize += afterSize

            //Remove absolute path from TinyPng file path info
            tinyPngInfo = new TinyPngInfo(relativePath, beforeSize, afterSize, generateMD5(imgFile))

            logger.info("beforeSize: $beforeSizeStr -> afterSize: ${afterSizeStr}")
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
                                                           String projParentDirPath, Logger logger) {
        TinyPngResult result = new TinyPngResult()
        //directory.e
        directory.eachFile(FileType.ANY) { file ->
            if(file.isDirectory()) {
                boolean exclude = false
                configuration.excludeDirs.each { dirPattern ->
                    if(file.path.endsWith(dirPattern)) {
                       exclude = true
                    }
                }
                if(exclude) {
                    logger.info("skipping directory ${file.path}\n");
                    //print("skipping directory ${file.path}\n")
                } else {
                    logger.info("scanning directory ${file.path} \n")
                    //print("scanning directory ${file.path} \n")
                    result.addResult(scanDirectoryForImageFiles(file, compressedList, newCompressedList, configuration, projParentDirPath, logger));
                }
            } else if(file.isFile()) {
                configuration.resourcePattern.each { pattern ->
                    if(file.getName().matches(~/$pattern/)) {
                        def imgFile = file
                        TinyPngInfo info = compress(imgFile, configuration.whiteList, compressedList, projParentDirPath, logger)
                        if (info != null) {
                            result.addInfo(info)
                            //beforeSize += result.preSize
                            //afterSize += result.postSize
                            //error = result.error
                            newCompressedList.add(info)
                        } else {
                            logger.debug("${imgFile} returning null\n")
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
            logger.lifecycle("Tiny API Key not set")
            //println("Tiny API Key not set")
            return
        }

        def apiKey = configuration.apiKey
        try {
            Tinify.setKey("${apiKey}")
            Tinify.validate()
        } catch (Exception ignored) {
            logger.lifecycle("Tiny Validation of API key failed.")
            //println("Tiny Validation of API key failed.")
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
                    logger.lifecycle("compressed-resource.json is invalid, ignore")
                    //println("compressed-resource.json is invalid, ignore")
                }
            } catch (Exception ignored) {
                logger.lifecycle("compressed-resource.json is invalid, ignore")
                //println("compressed-resource.json is invalid, ignore")
            }
        }

        TinyPngResult finalResult  = null
        def newCompressedList = new ArrayList<TinyPngInfo>()
        def rootDir = new File("${project.projectDir}")
        def projParentDirPath = rootDir.getParentFile().getPath();
        logger.debug("project root set to ... ${rootDir}")
        //println("project root set to ... ${rootDir}")
        if(rootDir.exists() && rootDir.isDirectory()) {
            if (!(configuration.resourcePattern ?: false)) {
                configuration.resourcePattern = [".+\\.png", ".+\\.jpg", ".+\\.jpeg"]
            }
            finalResult = scanDirectoryForImageFiles(rootDir, compressedList, newCompressedList, configuration, projParentDirPath, logger);
        }

        if(newCompressedList) {
            for (TinyPngInfo newTinyPng : newCompressedList) {
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
            logger.lifecycle("Task finish, compress ${newCompressedList.size()} files, before total size: ${formatFileSize(finalResult.beforeSize)} after total size: ${formatFileSize(finalResult.afterSize)}")
        }
    }
}