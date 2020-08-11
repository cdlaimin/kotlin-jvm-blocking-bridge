package net.mamoe.kjbb.compiler.backend.ir

import net.mamoe.kjbb.compiler.backend.jvm.followedBy
import net.mamoe.kjbb.compiler.backend.jvm.isGeneratedBlockingBridgeStub
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat

/**
 * For top-level functions
 */
class JvmBlockingBridgeFileLoweringPass(
    private val context: IrPluginContext,
) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context)
        }
    }
}

internal fun IrDeclaration.transformFlat(context: IrPluginContext): List<IrDeclaration> {
    val declaration = this
    if (declaration is IrSimpleFunction) {
        if (declaration.descriptor.isGeneratedBlockingBridgeStub())
            return listOf()

        if (declaration.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME)) {
            check(declaration.canGenerateJvmBlockingBridge().diagnosticPassed) {
                // TODO: 2020/7/8 DIAGNOSTICS
                "@JvmBlockingBridge is not applicable to function '${declaration.name}'"
            }
            if (declaration.isFakeOverride || declaration.overriddenSymbols
                    .any { it is IrSimpleFunction && it.hasAnnotation(JVM_BLOCKING_BRIDGE_FQ_NAME) }
            ) {
                return listOf(declaration)
            }
            check(!declaration.hasDuplicateBridgeFunction()) {
                // TODO: 2020/7/8 DIAGNOSTICS FROM PLATFORM_DECLARE_CLASH
                "PLATFORM_DECLARE_CLASH: function '${declaration.name}'"
            }
            return declaration.followedBy(
                context.generateJvmBlockingBridges(
                    declaration
                )
            )
        }
    }

    return listOf(declaration)
}

/**
 * For in-class functions
 */
class JvmBlockingBridgeClassLoweringPass(
    private val context: IrPluginContext,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        irClass.transformDeclarationsFlat { declaration ->
            declaration.transformFlat(context)
        }
    }
}