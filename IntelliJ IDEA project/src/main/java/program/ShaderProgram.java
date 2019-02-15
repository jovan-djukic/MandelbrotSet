package program;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import com.jogamp.opengl.GL4;

import shaders.Shader;

public class ShaderProgram {
	private static class Messages {
		public static final String	shaderProgramBuildLog	= "Shader program %s build log: ";
		public static final String	shaderProgramNotCreated	= "Shader program %s not created" + System.lineSeparator();
		public static final String	compilingShader			= "Compiling shader %s " + System.lineSeparator();
		public static final String	programLinkStatus		= "Program %s link status: ";
		public static final String	success					= "Success";
		public static final String	failure					= "Failure";
		public static final String	programUniformNotFound	= "Shader program %s : uniform %s not found" + System.lineSeparator();
	}
	
	public enum Status {
		PROGRAM_NOT_COMPLETE, PROGRAM_COMPLETE
	};

	private Dictionary<String, Integer>	uniforms;
	private ArrayList<Shader>			shaders;
	private int							programID;
	private String						name;
	private StringBuilder				buildLog;
	private Status						programStatus;

	public ShaderProgram(String name) {
		this.name = name;

		this.uniforms = new Hashtable<>();
		this.shaders = new ArrayList<>();
	}

	public ShaderProgram addShader(Shader shader) {
		this.shaders.add(shader);
		return this;
	}

	public int getProgramID() {
		return this.programID;
	}

	public Status getProgramStatus() {
		return this.programStatus;
	}

	public String getBuildLog() {
		return this.buildLog.toString();
	}

	public int getUniformLocation(String uniformName) {
		Integer location = this.uniforms.get(uniformName);

		if (location == null) {
			return -1;
		} else {
			return location.intValue();
		}
	}

	public void build(GL4 gl, ArrayList<String> listOfUniforms) {
		if (this.programID == 0) {
			this.programID = gl.glCreateProgram();
		}

		this.programStatus = Status.PROGRAM_NOT_COMPLETE;

		this.buildLog = new StringBuilder();
		this.buildLog.append(String.format(Messages.shaderProgramBuildLog, this.name));

		boolean allSuccessful = true;

		for (Shader shader : this.shaders) {
			if (Shader.Status.UNCOMPILED.equals(shader.getCompilationStatus()) || Shader.Status.UNINITIALIZED.equals(shader.getCompilationStatus())) {
				this.buildLog.append(String.format(Messages.compilingShader, shader.getShaderName()));
				shader.build(gl);
				this.buildLog.append(shader.getCompilationLog());
			}

			if (!Shader.Status.COMPILED_SUCCESS.equals(shader.getCompilationStatus())) {
				this.buildLog.append(shader.getCompilationStatus()).append(System.lineSeparator());
				allSuccessful = false;
			}
		}

		if (!allSuccessful) {
			this.buildLog.append(String.format(Messages.shaderProgramNotCreated, this.name));
			gl.glDeleteProgram(this.programID);
			return;
		}

		for (Shader shader : this.shaders) {
			gl.glAttachShader(this.programID, shader.getShaderID());
		}

		gl.glLinkProgram(this.programID);

		int[] params = new int[1];

		gl.glGetProgramiv(this.programID, GL4.GL_LINK_STATUS, params, 0);

		this.buildLog.append(String.format(Messages.programLinkStatus, this.name));

		if (params[0] == GL4.GL_TRUE) {
			this.buildLog.append(Messages.success);
			this.programStatus = Status.PROGRAM_COMPLETE;
		} else {
			this.buildLog.append(Messages.failure);
		}

		this.buildLog.append(System.lineSeparator());

		if (Status.PROGRAM_COMPLETE.equals(this.programStatus)) {
			for (String uniform : listOfUniforms) {
				int location = gl.glGetUniformLocation(this.programID, uniform);
				if (location > -1) {
					this.uniforms.put(uniform, location);
				} else {
					this.buildLog.append(String.format(Messages.programUniformNotFound, this.name, uniform));
				}
			}
		}

		gl.glGetProgramiv(this.programID, GL4.GL_INFO_LOG_LENGTH, params, 0);
		int infoLogLength = params[0];
		ByteBuffer infoLog = ByteBuffer.allocate(infoLogLength);
		gl.glGetProgramInfoLog(this.programID, infoLogLength, null, infoLog);
		this.buildLog.append(new String(infoLog.array())).append(System.lineSeparator());
	}

	public void build(GL4 gl) {
		this.build(gl, new ArrayList<>());
	}

	public void delete(GL4 gl) {
		if (this.programID != 0) {
			for (Shader shader : this.shaders) {
				shader.delete(gl);
			}

			gl.glDeleteProgram(this.programID);
			this.programID = 0;
		}
	}
}
