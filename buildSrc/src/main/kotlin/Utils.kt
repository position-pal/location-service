object Utils {

    val inCI: Boolean
        get() = System.getenv()["CI"].equals("true", ignoreCase = true)
}
