package zxs.mirai

import coreLibrary.lib.util.loop

var ok = true

 onEnable {
     launch {
         loop {
             delay(3000)
             ok = true
         }
     }
 }

fun canSend():Boolean {
    if (ok) {
        ok = false
        return true
    } else return false
}