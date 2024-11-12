object Utils {

    val isInCI: Boolean
        get() = System.getenv()["CI"].equals("true", ignoreCase = true)
}
