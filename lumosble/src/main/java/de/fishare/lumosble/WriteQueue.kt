package de.fishare.lumosble

import java.util.*

/**
 * Created by yyl on 26/06/2017.
 */
class WriteQueue :LinkedList<WriteQueue.WritingRunnable>(){
    override fun offer(w: WritingRunnable?): Boolean {
        val result = super.offer(w)
        if (size == 1) write()
        return result
    }

    fun write(){
        element()?.writeAction()
    }

    interface WritingRunnable{
        fun writeAction()
    }

}


