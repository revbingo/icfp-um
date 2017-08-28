
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class UniversalMachineKtTest {

    @Test
    fun platterGivesCorrectOpCodeAndRegisters() {
        var unit = instruction(5,1,4,7)

        assertThat(unit.operator, equalTo(5))
        assertThat(unit.reg_a, equalTo(1))
        assertThat(unit.reg_b, equalTo(4))
        assertThat(unit.reg_c, equalTo(7))

        unit = instruction(10,3,5,6)

        assertThat(unit.operator, equalTo(10))
        assertThat(unit.reg_a, equalTo(3))
        assertThat(unit.reg_b, equalTo(5))
        assertThat(unit.reg_c, equalTo(6))

        unit = instruction(15,7,7,7)

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

        unit.memory[100L] = mutableListOf(1L, 2L, 3L)

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

        unit.memory[100L] = mutableListOf(1L, 2L, 3L)

        unit.setRegister(0, 100)
        unit.setRegister(1, 2)
        unit.setRegister(2, 9999)
        unit.processPlatter(instruction(2, 0, 1, 2))

        assertThat(unit.memory[100L]?.get(2), equalTo(9999L))
    }
}

fun instruction(opCode: Int, a: Int, b: Int, c: Int): Platter {
    var value = opCode.toLong() shl 28

    value = value xor (a.toLong() shl 6)
    value = value xor (b.toLong() shl 3)
    value = value xor (c.toLong())
    return Platter(value)
}