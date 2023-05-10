package jp.henry.uke.mask

import picocli.CommandLine
import java.util.Properties

class PropertiesVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> =
        this.javaClass.getResourceAsStream("metadata.properties").use {
            val prop = Properties()
            prop.load(it)
            arrayOf(prop.getProperty("version") ?: "SNAPSHOT")
        }
}
