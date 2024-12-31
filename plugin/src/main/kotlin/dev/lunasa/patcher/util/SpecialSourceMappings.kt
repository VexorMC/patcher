package dev.lunasa.patcher.util

import net.fabricmc.tinyremapper.IMappingProvider
import java.io.BufferedReader

class SpecialSourceMappings(private val srgMappings: BufferedReader) : IMappingProvider {
    override fun load(out: IMappingProvider.MappingAcceptor) {
        srgMappings.forEachLine { line ->
            val parts = line.split(" ")
            when (parts[0]) {
                "CL:" -> {
                    // Class mapping: CL: original_name mapped_name
                    val srcName = parts[1].replace('.', '/')
                    val dstName = parts[2].replace('.', '/')
                    out.acceptClass(srcName, dstName)
                }
                "FD:" -> {
                    // Field mapping: FD: original_class/original_field mapped_class/mapped_field
                    val srcFieldParts = parts[1].split("/")
                    val dstFieldParts = parts[2].split("/")
                    val owner = srcFieldParts.dropLast(1).joinToString("/")
                    val fieldName = srcFieldParts.last()
                    val mappedFieldName = dstFieldParts.last()
                    out.acceptField(IMappingProvider.Member(owner, fieldName, ""), mappedFieldName)
                }
                "MD:" -> {
                    // Method mapping: MD: original_class/original_method original_desc mapped_class/mapped_method mapped_desc
                    val srcMethodParts = parts[1].split("/")
                    val originalDesc = parts[2]
                    val mappedMethodName = parts[4]
                    val owner = srcMethodParts.dropLast(1).joinToString("/")
                    val methodName = srcMethodParts.last()
                    out.acceptMethod(
                        IMappingProvider.Member(owner, methodName, originalDesc),
                        mappedMethodName
                    )
                }
            }
        }
    }
}
