
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

class FailException(message: String): RuntimeException(message)
class Halt: RuntimeException()

fun main(args: Array<String>) {
    println("Starting UniversalMachine")

    val um = UniversalMachine()
    um.init(File("sandmark.umz"))
    um.start()
}

class UniversalMachine {
    val registers = Array(8, {0L})

    val memory = mutableMapOf<Long, Array<Long>>();
    var output: Char? = null

    var executionFinger = 0

    fun init(file: File) {
       loadProgram(Loader.loadBinary(file))
    }

    fun start() {
        while(true) {
            output = null
            val program = memory[0L] ?: failure("Program location 0L has been abandoned")
            printStatus()
            val exit = processPlatter(program.get(executionFinger++))
            if(exit) break;

            if(output != null) print(output!!)

            if(executionFinger > program.size - 1) break;
        }
    }

    fun printStatus() {
        val inst = platterFor(memory[0L]!!.get(executionFinger))
        System.err.println("""I:${inst}
                0:${registers[0]} / 1:${registers[1]} / 2:${registers[2]} / 3:${registers[3]}
                4:${registers[4]} / 5:${registers[5]} / 6:${registers[6]} / 7:${registers[7]}""".trimIndent())
    }

    fun failure(message: String): Nothing {
        println("FAILURE: ${message}")
        throw FailException(message)
    }

    fun abandon(address: Long) {
        memory.remove(address)
    }

    fun processPlatter(value: Long): Boolean {
        val platter = StandardPlatter(value)
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
                if(registers[c] == 0L) failure("Division by zero")
                registers[a] = registers[b] / registers[c]
            }
            6 -> { /* Not-And.

                  Each bit in the register A receives the 1 bit if
                  either register B or register C has a 0 bit in that
                  position.  Otherwise the bit in register A receives
                  the 0 bit. */

                registers[a] = (registers[b] and registers[c]).inv() and 0x00000000FFFFFFFF
            }
            7 -> { /* Halt.

                  The universal machine stops computation. */
                return true
            }
            8 -> { /* Allocation.

                  A new array is created with a capacity of platters
                  commensurate to the value in the register C. This
                  new array is initialized entirely with platters
                  holding the value 0. A bit pattern not consisting of
                  exclusively the 0 bit, and that identifies no other
                  active allocated array, is placed in the B register. */
                val addr = registers[b]
                if(addr == 0L) throw FailException("Attempt to allocate location 0")
                if(memory.containsKey(addr)) failure("Attempt to reallocate existing platters ${addr}")
                memory[addr] = Array(registers[c].toInt(), {0L})
            }
            9 -> { /*  Abandonment.

                  The array identified by the register C is abandoned.
                  Future allocations may then reuse that identifier. */
                val addr = registers[c]
                if(!memory.containsKey(addr)) failure("Attempt to abandon non-active location ${addr}")
                memory.remove(addr)

            }
            10 -> { /* Output.

                  The value in the register C is displayed on the console
                  immediately. Only values between and including 0 and 255
                  are allowed. */
                if(!(registers[c] in 0..255)) failure("Attempt to display non-8bit character")
                output = registers[c].toChar()

            }
            11 -> { /* Input.

                  The universal machine waits for input on the console.
                  When input arrives, the register C is loaded with the
                  input, which must be between and including 0 and 255.
                  If the end of input has been signaled, then the
                  register C is endowed with a uniform value pattern
                  where every place is pregnant with the 1 bit. */
                val inStream = System.`in`
                val input = inStream.read()
                registers[c] = input.toLong()
            }
            12 -> { /* Load Program.

                  The array identified by the B register is duplicated
                  and the duplicate shall replace the '0' array,
                  regardless of size. The execution finger is placed
                  to indicate the platter of this array that is
                  described by the offset given in C, where the value
                  0 denotes the first platter, 1 the second, et
                  cetera.

                  The '0' array shall be the most sublime choice for
                  loading, and shall be handled with the utmost
                  velocity. */
                println("SWITCHING PROGRAM")
                val addr = registers[b]
                if(!memory.containsKey(addr)) failure("Attempt to load from non-existent location ${addr}")
                memory.put(0L, memory[addr]!!.clone())
                executionFinger = registers[c].toInt()
            }
            13 -> { /* Orthography.

                  The value indicated is loaded into the register A
                  forthwith. */
                val orth = OrthoPlatter(value)
                registers[orth.reg_a] = orth.value

            }
            else -> throw FailException("Unimplemented operator ${platter.operator}")
        }
        return false
    }

    fun loadProgram(values: List<Long>) {
        memory.put(0L, values.toTypedArray())
    }

    fun setRegister(reg: Int, value: Long) {
        registers[reg] = value
    }
}

object Loader {
    fun load(file: File): List<Long> {
        val lines = file.readLines()

        return load(lines)
    }

    fun load(lines: List<String>): List<Long> {
        val words = lines.flatMap { line ->
            line.split(" ")
        }

        val instructions = mutableListOf<Long>()
        for(i in (0..words.size - 2).step(2)) {
            val value = java.lang.Long.parseUnsignedLong("${words[i]}${words[i+1]}", 16)
            instructions.add(value)
        }

        return instructions
    }

    fun loadBinary(file: File): List<Long> {
        val input = DataInputStream(FileInputStream(file))
        val instructions = mutableListOf<Long>()

        while(input.available() > 0) {
            val highBytes = input.readUnsignedShort()
            val lowBytes = input.readUnsignedShort()
            val value = (highBytes.toLong() shl 16) or lowBytes.toLong()
            instructions.add(value)
        }
        input.close()
        return instructions
    }
}

fun Long.overflow() = this.rem(Math.pow(2.0,32.0).toLong())

fun platterFor(value: Long): Platter = when((value ushr 28).toInt()) {
    in 0..12 -> StandardPlatter(value)
    else -> OrthoPlatter(value)
}

open class Platter(val input: Long) {
    val operator = (input ushr 28).toInt()
}

class StandardPlatter(input: Long): Platter(input) {
    val reg_c = (input and 7).toInt()
    val reg_b = ((input ushr 3) and 7).toInt()
    val reg_a = ((input ushr 6) and 7).toInt()
    override fun toString(): String {
        return "(${operator},${reg_a},${reg_b},${reg_c})"
    }
}
class OrthoPlatter(input: Long): Platter(input) {
    val reg_a = ((input ushr 25) and 7).toInt()
    val value = input and 0x1FFFFFF

    override fun toString(): String {
        return "($operator,$reg_a,$value)"
    }
}