@file:JvmBlockingBridge

import net.mamoe.kjbb.JvmBlockingBridge

suspend fun test() {

}

@JvmBlockingBridge // should error
suspend fun test2() {

}