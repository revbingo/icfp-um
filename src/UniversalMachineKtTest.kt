
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.fail

class UniversalMachineKtTest {

    @Test
    fun platterGivesCorrectOpCodeAndRegisters() {
        var unit = Platter(instruction(5,1,4,7))

        assertThat(unit.operator, equalTo(5))
        assertThat(unit.reg_a, equalTo(1))
        assertThat(unit.reg_b, equalTo(4))
        assertThat(unit.reg_c, equalTo(7))

        unit = Platter(instruction(10,3,5,6))

        assertThat(unit.operator, equalTo(10))
        assertThat(unit.reg_a, equalTo(3))
        assertThat(unit.reg_b, equalTo(5))
        assertThat(unit.reg_c, equalTo(6))

        unit = Platter(instruction(15,7,7,7))

        assertThat(unit.operator, equalTo(15))
        assertThat(unit.reg_a, equalTo(7))
        assertThat(unit.reg_b, equalTo(7))
        assertThat(unit.reg_c, equalTo(7))

    }

    @Test
    fun conditionalMove_DoesNotMoveIf_C_IsZero() {
        val unit = UniversalMachine()

        unit.setRegister(0, 1)
        unit.setRegister(1, 3)
        unit.setRegister(2, 0)

        unit.processPlatter(instruction(0, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(1L))
    }

    @Test
    fun conditionalMove_MovesIf_C_IsNotZero() {
        val unit = UniversalMachine()

        unit.setRegister(0, 1)
        unit.setRegister(1, 3)
        unit.setRegister(2, 1)

        unit.processPlatter(instruction(0, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(3L))
    }

    @Test
    fun additionAdds_B_and_C_Into_A() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 3)
        unit.setRegister(2, 1)

        unit.processPlatter(instruction(3, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(4L))
    }

    @Test
    fun addition_overflows_mod_2pow32() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, Math.pow(2.0, 32.0).minus(1).toLong())
        unit.setRegister(2, 3)

        unit.processPlatter(instruction(3, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(2L))
    }

    @Test
    fun multiplication_overflows_mod_2pow32() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, Math.pow(2.0, 31.0).plus(1).toLong())
        unit.setRegister(2, 2)

        unit.processPlatter(instruction(4, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(2L))
    }

    @Test
    fun multiplication_multiples_B_and_C_into_A() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 3)
        unit.setRegister(2, 9)

        unit.processPlatter(instruction(4, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(27L))
    }

    @Test(expected = FailException::class)
    fun machineFailsIfProgramMemoryIsAbandoned() {
        val unit = UniversalMachine()

        unit.abandon(0L)

        unit.start()
    }

    @Test
    fun array_index_A_receives_value_at_offset_C_in_array_B() {

        val unit = UniversalMachine()

        unit.memory[100L] = arrayOf(1L, 2L, 3L)

        unit.setRegister(0, 0)
        unit.setRegister(1, 100)
        unit.setRegister(2, 2)
        unit.processPlatter(instruction(1, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(3L))
    }

    @Test(expected = FailException::class)
    fun machine_fails_if_index_references_unallocated_mem_location() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 100)
        unit.setRegister(2, 2)
        unit.processPlatter(instruction(1, 0, 1, 2))
    }

    @Test
    fun array_amendment_updates_value_at_A_offset_B_with_value_in_C() {
        val unit = UniversalMachine()

        unit.memory[100L] = arrayOf(1L, 2L, 3L)

        unit.setRegister(0, 100)
        unit.setRegister(1, 2)
        unit.setRegister(2, 9999)
        unit.processPlatter(instruction(2, 0, 1, 2))

        assertThat(unit.memory[100L]?.get(2), equalTo(9999L))
    }

    @Test
    fun division_divides_B_by_C_and_puts_in_A() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 30)
        unit.setRegister(2, 5)

        unit.processPlatter(instruction(5, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(6L))
    }

    @Test(expected = FailException::class)
    fun division_fails_if_register_C_is_0() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 30)
        unit.setRegister(2, 0)

        unit.processPlatter(instruction(5, 0, 1, 2))
    }

    @Test
    fun not_and_does_a_nand_on_B_and_C_with_result_in_A() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 0b00000000_11111111_00000000_11111111)
        unit.setRegister(2, 0b00000000_00000000_11111111_11111111)

        unit.processPlatter(instruction(6, 0, 1, 2))

        assertThat(unit.registers[0], equalTo(0b11111111_11111111_11111111_00000000))
    }

    @Test
    fun halt_stops_the_machine() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 3)
        unit.setRegister(2, 5)

        unit.loadProgram(listOf(instruction(7), instruction(3, 0, 1, 2)))

        unit.start()

        assertThat(unit.registers[0], equalTo(0L))
    }

    @Test
    fun execution_stops_when_finger_points_beyond_array() {
        val unit = UniversalMachine()

        unit.loadProgram(listOf(instruction(3, 0, 1, 2), instruction(3, 0, 1, 2)))

        try {
            unit.start()
        } catch(e: Exception) {
            fail("Went beyond end of array")
        }
    }

    @Test(expected = FailException::class)
    fun fails_on_unknown_operator() {
        val unit = UniversalMachine()

        unit.processPlatter(instruction(100, 0, 1, 2))
    }

    @Test
    fun allocation_allocates_memory_at_B_with_array_of_size_C() {
        val unit = UniversalMachine()

        unit.setRegister(1, 100L)
        unit.setRegister(2, 5L)
        assertThat(unit.memory[100L], nullValue())
        unit.processPlatter(instruction(8, 0, 1, 2))

        assertThat(unit.memory[100L], notNullValue())
        assertThat(unit.memory[100L]?.size, equalTo(5))

    }

    @Test(expected = FailException::class)
    fun allocation_fails_if_tries_to_allocate_location_zero() {
        val unit = UniversalMachine()

        unit.setRegister(1, 0L)
        unit.setRegister(2, 5L)

        unit.processPlatter(instruction(8, 0, 1, 2))

    }

    @Test
    fun abandonment_frees_up_memory_addressed_by_C() {
        val unit = UniversalMachine()

        unit.memory[100L] = arrayOf(1L,2L,3L)

        unit.setRegister(1, 100L)

        unit.processPlatter(instruction(9, 0, 0, 1))

        assertThat(unit.memory[100L], nullValue())
    }

    @Test(expected = FailException::class)
    fun abandonment_fails_if_referencing_a_non_active_location() {
        val unit = UniversalMachine()

        unit.memory[100L] = arrayOf(1L,2L,3L)

        unit.setRegister(1, 50L)

        unit.processPlatter(instruction(9, 0, 0, 1))
    }

    @Test
    fun output_sets_output_to_be_displayed() {
        val unit = UniversalMachine()

        unit.setRegister(7, 65)

        unit.processPlatter(instruction(10, 0, 0, 7))

        assertThat(unit.output, equalTo('A'))
    }

    @Test(expected = FailException::class)
    fun output_fails_if_value_outside_ascii_range() {
        val unit = UniversalMachine()

        unit.setRegister(7, 256)

        unit.processPlatter(instruction(10, 0, 0, 7))

    }

    @Test
    fun input_loads_register_C_with_value() {
        val unit = UniversalMachine()

        val mockInputStream = ByteArrayInputStream(ByteArray(1,{65}))
        System.setIn(mockInputStream)

        unit.processPlatter(instruction(11, 0, 0, 1))

        assertThat(unit.registers[1], equalTo(65L))
    }

    @Test
    fun load_program_duplicates_array_to_location_zero_and_sets_execution_finger() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)
        unit.setRegister(1, 3)
        unit.setRegister(2, 1)
        unit.setRegister(3, 100)
        unit.setRegister(4, 1)

        unit.memory[100L] = arrayOf(instruction(7, 0, 0, 0), instruction(3, 0, 1, 2))

        unit.processPlatter(instruction(12, 0, 3, 4))

        assertThat(unit.executionFinger, equalTo(1))
        assertThat(unit.memory[0L]?.size, equalTo(2))
    }

    @Test
    fun orthography_loads_value_into_register() {
        val unit = UniversalMachine()

        unit.setRegister(0, 0)

        unit.processPlatter(loadInstruction(0, 35267))

        assertThat(unit.registers[0], equalTo(35267L))
    }
}

fun instruction(opCode: Int, a: Int = 0, b: Int = 0, c: Int = 0 ): Long {
    var value = opCode.toLong() shl 28

    value = value xor (a.toLong() shl 6)
    value = value xor (b.toLong() shl 3)
    value = value xor (c.toLong())
    return value
}

fun loadInstruction(register: Int, newValue: Long): Long {
    var value = 13.toLong() shl 28

    value = value xor (register.toLong() shl 25)
    value = value xor (newValue)
    return value
}