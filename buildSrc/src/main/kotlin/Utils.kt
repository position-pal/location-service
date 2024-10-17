object Utils {

    fun inCI(): Boolean = System.getenv()["CI"].equals("true", ignoreCase = true)
}
