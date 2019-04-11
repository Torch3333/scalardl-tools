package com.scalar.client.tool.explorer.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.client.config.ClientConfig;
import com.scalar.client.service.ClientModule;
import com.scalar.client.service.ClientService;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import picocli.CommandLine;

@CommandLine.Command(
    name = "explorer",
    description = "Scalar DL Explorer",
    version = "1.0",
    subcommands = {Get.class, Scan.class, Validate.class, ListContracts.class})
public class Explorer implements Runnable {

  @CommandLine.Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Display this help and exit")
  private boolean help;

  public static void main(String[] args) {
    String[] commandArgs = args.length != 0 ? args : new String[] {"--help"};
    CommandLine.run(new Explorer(), commandArgs);
  }

  @FunctionalInterface
  public interface ExplorerExecutor {
    void execute(com.scalar.client.tool.explorer.Explorer explorer);
  }

  public void execute(String file, ExplorerExecutor executor) {
    try {
      ClientConfig config = new ClientConfig(new FileInputStream(file));
      Injector injector = Guice.createInjector(new ClientModule(config));
      try (ClientService clientService = injector.getInstance(ClientService.class)) {
        com.scalar.client.tool.explorer.Explorer explorer =
            new com.scalar.client.tool.explorer.Explorer(clientService, config);
        executor.execute(explorer);
      }
    } catch (IOException e) {
      System.err.println("Failed to load " + file);
    }
  }

  public void output(JsonStructure structure, String format) throws Exception {
    switch (format) {
      case "yaml":
        JsonNode jsonNode = new ObjectMapper().readTree(structure.toString());
        YAMLMapper yamlMapper = new YAMLMapper();
        yamlMapper.configure(Feature.WRITE_DOC_START_MARKER, false);
        System.out.println(yamlMapper.writeValueAsString(jsonNode));
        break;
      case "json":
        Map<String, Object> config = new HashMap<>(1);
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory factory = Json.createWriterFactory(config);
        JsonWriter writer = factory.createWriter(System.out);
        writer.write(structure);
        System.out.println("");
        writer.close();
        break;
    }
  }

  @Override
  public void run() {}
}
