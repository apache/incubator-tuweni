package org.apache.tuweni.relayer

import picocli.CommandLine

internal class RelayerAppCommandlineArguments() {
  @CommandLine.Option(names = ["-b", "--bind"], description = ["Endpoint to bind to"]) var bind: String = ""
  @CommandLine.Option(names = ["-t", "--to"], description = ["Endpoint to relay to"]) var to: String = ""
  @CommandLine.Option(names = ["-h", "--help"], description = ["Prints usage prompt"]) var help: Boolean = false
}
