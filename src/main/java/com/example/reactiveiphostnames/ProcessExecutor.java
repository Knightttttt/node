package com.example.reactiveiphostnames;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
@Component
public class ProcessExecutor {

	public Mono<ProcessOutput> execute(String command) {
		return Mono.fromCallable(() -> {
			Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
			return new ProcessOutput(
					toString(process.getInputStream()),
					toString(process.getErrorStream())
			);
		});
	}

	private String toString(InputStream inputStream) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

	public static class ProcessOutput {
		private final String stdout;
		private final String stderr;

		public ProcessOutput(String stdout, String stderr) {
			this.stdout = stdout;
			this.stderr = stderr;
		}

		public String getStdout() {
			return stdout;
		}

		public String getStderr() {
			return stderr;
		}
	}
}
