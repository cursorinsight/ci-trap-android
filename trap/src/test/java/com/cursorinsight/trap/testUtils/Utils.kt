package com.cursorinsight.trap.testUtils

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class Utils {
    companion object {
        @Throws(Exception::class)
        fun setFinalStatic(field: Field, value: Any) {
            field.isAccessible = true
            getModifiersField().also {
                it.isAccessible = true
                it.setInt(field, field.modifiers and Modifier.FINAL.inv())
            }
            field.set(null, value)
        }

        private fun getModifiersField(): Field {
            return try {
                Field::class.java.getDeclaredField("modifiers")
            } catch (e: NoSuchFieldException) {
                try {
                    val getDeclaredFields0: Method =
                        Class::class.java.getDeclaredMethod(
                            "getDeclaredFields0",
                            Boolean::class.javaPrimitiveType
                        )
                    getDeclaredFields0.isAccessible = true
                    val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
                    for (field in fields) {
                        if ("modifiers" == field.name) {
                            return field
                        }
                    }
                } catch (ex: ReflectiveOperationException) {
                    e.addSuppressed(ex)
                }
                throw e
            }
        }
    }
}