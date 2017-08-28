class FailException(message: String): RuntimeException(message)

fun main(args: Array<String>) {
    print("Starting UniversalMachine")

    val um = UniversalMachine()
    um.init()
    um.start()
}

class UniversalMachine {
    val registers = Array(8, {0L})

    val memory = mutableMapOf<Long, MutableList<Long>>();

    var output = String()

    var executionFinger = 0

    fun init() {
        val program = loadProgram(emptyList())
        memory.put(0L, program)
    }

    fun start() {
        while(true) {
            val program = memory[0L] ?: failure("Program location 0L has been abandoned")
            val platter = Platter(program.get(executionFinger))
            processPlatter(platter)

            executionFinger++
        }
    }

    fun failure(message: String): Nothing {
        println("FAILURE: ${message}")
        throw FailException(message)
    }

    fun abandon(address: Long) {
        memory.remove(address)
    }

    fun processPlatter(platter: Platter) {
        val a = platter.reg_a
        val b = platter.reg_b
        val c = platter.reg_c

        when(platter.operator) {
            0 -> {  /* Conditional Move
                    The register A receives the value in register B,
                    unless the register C contains 0. */
                if(registers[c] != 0L) {
                    registers[a] = registers[b]
                }
            }
            1 -> { /* Array Index
                    The register A receives the value stored at offset
                  in register C in the array identified by B. */
                val memArray = memory.get(registers[b]) ?: failure("Indexed non-allocated memory location ${registers[b]}")
                registers[a] = memArray[registers[c].toInt()]

            }
            2 -> { /* Array Amendment
                    The array identified by A is amended at the offset
                    in register B to store the value in register C. */
                val memArray = memory.get(registers[a]) ?: failure("Indexed non-allocated memory location ${registers[a]}")

                memArray.set(registers[b].toInt(), registers[c])
            }

            3 -> { /* Addition
                    The register A receives the value in register B plus
                    the value in register C, modulo 2^32. */
                registers[a] = (registers[b] + registers[c]).overflow()
            }
            4 -> { /*Multiplication.

                  The register A receives the value in register B times
                  the value in register C, modulo 2^32. */
                registers[a] = (registers[b] * registers[c]).overflow()
            }
            5 -> { /* Division.

                  The register A receives the value in register B
                  divided by the value in register C, if any, where
                  each quantity is treated treated as an unsigned 32
                  bit number. */

            }
            6 -> { /* Not-And.

                  Each bit in the register A receives the 1 bit if
                  either register B or register C has a 0 bit in that
                  position.  Otherwise the bit in register A receives
                  the 0 bit. */

                registers[a] = (registers[b] and registers[c]).inv()
            }
        }
    }

    fun loadProgram(values: List<Long>): MutableList<Long> {
        return values.toMutableList()
    }

    fun setRegister(reg: Int, value: Long) {
        registers[reg] = value
    }
}

fun Long.overflow() = this.rem(Math.pow(2.0,32.0).toLong())

data class Platter(var value: Long) {
    val operator = (value ushr 28).toInt()
    val reg_c = (value and 7).toInt()
    val reg_b = ((value ushr 3) and 7).toInt()
    val reg_a = ((value ushr 6) and 7).toInt()
}