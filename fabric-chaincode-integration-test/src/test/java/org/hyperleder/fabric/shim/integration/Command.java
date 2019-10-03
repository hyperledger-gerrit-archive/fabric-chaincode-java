/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperleder.fabric.shim.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Command {

	protected List<String> cmd;
	protected Map<String, String> env;

	Command(List<String> cmd) {
		this.cmd = cmd;
		this.env = new HashMap<>();

	}
	
	class Result {
		ArrayList<String> stdout;
		ArrayList<String> stderr;
		int exitcode;
	}

	public Result run() {
		return this.run(false);
	}

	public Result run(boolean quiet) {

		ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		processBuilder.environment().putAll(env);
		final Result result = new Result();
		
		System.out.println("Running:" + this.toString());
		try {
			Process process = processBuilder.start();
			
			CompletableFuture<ArrayList<String>> soutFut = readOutStream(process.getInputStream(),quiet?null:System.out);
			CompletableFuture<ArrayList<String>> serrFut = readOutStream(process.getErrorStream(),quiet?null:System.err);
			
			CompletableFuture<Result> resultFut = soutFut.thenCombine(serrFut, (stdout, stderr) -> {
		         // print to current stderr the stderr of process and return the stdout				
				result.stderr = stderr;
				result.stdout = stdout;
		        return result;
		     });
			
			result.exitcode = process.waitFor();
			// get stdout once ready, blocking
			resultFut.get();

		} catch (IOException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result.exitcode = -1;
		}

		return result;
	}

	/**
	 * 
	 * @param is
	 * @param stream
	 * @return
	 */
	CompletableFuture<ArrayList<String>> readOutStream(InputStream is, PrintStream stream) {
		return CompletableFuture.supplyAsync(() -> {
			try (InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr);) {
				// StringBuilder res = new StringBuilder();
				ArrayList<String> res = new ArrayList<String>();
				String inputLine;
				while ((inputLine = br.readLine()) != null) {
					if (stream!=null) stream.println(inputLine);
					res.add(inputLine);//.append(System.lineSeparator());
				}
				return res;
			} catch (Throwable e) {
				throw new RuntimeException("problem with executing program", e);
			}
		});
	}

	public String toString() {
		return "[" + String.join(" ", cmd) + "]";
	}

	static public class Builder<T extends Command> implements Cloneable {
		public Builder<T> duplicate() {
			try {
				return (Builder<T>) this.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
	}
}
