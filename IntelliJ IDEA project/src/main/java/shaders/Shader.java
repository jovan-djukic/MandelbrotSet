package shaders;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;

public abstract class Shader {
	private static class Messages {
		public static final String	cannotCreateShader	= "Error creating shader %s (%s)";
		public static final String	compilationStatus	= "Shader %s (%s) compilation status: ";
		public static final String	success				= "Success";
		public static final String	failure				= "Failure";
	}

	public enum Status {
		UNINITIALIZED, UNCOMPILED, COMPILED_FAILURE, COMPILED_SUCCESS, CANNOT_CREATE_OBJECT
	};

	private int				shaderObjectID;
	private int				shaderType;
	private String			shaderName;
	private String[]		shaderSource;
	private StringBuffer	compilationLog;
	private Status			compilationStatus;

	protected Shader(int shaderType, String shaderName) {
		this.shaderType = shaderType;
		this.shaderName = shaderName;

		this.compilationStatus = Status.UNINITIALIZED;
	}

	public String getShaderName() {
		return this.shaderName;
	}

	public Shader setShaderSource(String[] shaderSource) {
		if (shaderSource != null) {
			this.shaderSource = shaderSource;
			this.compilationStatus = Status.UNCOMPILED;
		}
		return this;
	}

	public Shader setShaderSource(String shaderSource) {
		String[] source = { shaderSource };
		return this.setShaderSource(source);
	}

	public String getCompilationLog() {
		return this.compilationLog.toString();
	}

	public int getShaderID() {
		return this.shaderObjectID;
	}

	public Status getCompilationStatus() {
		return this.compilationStatus;
	}

	public void build(GL4 gl) {
		if (!Status.UNCOMPILED.equals(this.compilationStatus)) {
			return;
		}

		this.compilationLog = new StringBuffer();

		if (this.shaderObjectID == 0) {
			this.shaderObjectID = gl.glCreateShader(this.shaderType);
			if (gl.glGetError() != GL.GL_NO_ERROR) {
				this.compilationLog.append(String.format(Messages.cannotCreateShader, this.shaderName, this.getClass().getName()));
				this.compilationStatus = Status.CANNOT_CREATE_OBJECT;
				return;
			}
		}

		gl.glShaderSource(this.shaderObjectID, this.shaderSource.length, this.shaderSource, null);
		gl.glCompileShader(this.shaderObjectID);

		int[] params = new int[1];

		gl.glGetShaderiv(this.shaderObjectID, GL4.GL_COMPILE_STATUS, params, 0);

		this.compilationLog.append(String.format(Messages.compilationStatus, this.shaderName, this.getClass().getName()));

		if (params[0] == 1) {
			this.compilationLog.append(Messages.success);
			this.compilationStatus = Status.COMPILED_SUCCESS;
		} else {
			this.compilationLog.append(Messages.failure);
			this.compilationStatus = Status.COMPILED_FAILURE;
		}

		this.compilationLog.append(System.lineSeparator());

		gl.glGetShaderiv(this.shaderObjectID, GL4.GL_INFO_LOG_LENGTH, params, 0);

		int infoLogLength = params[0];

		ByteBuffer shaderInfoLog = ByteBuffer.allocate(infoLogLength);
		gl.glGetShaderInfoLog(this.shaderObjectID, infoLogLength, null, shaderInfoLog);
		this.compilationLog.append(new String(shaderInfoLog.array())).append(System.lineSeparator());
	}

	public void delete(GL4 gl) {
		gl.glDeleteShader(this.shaderObjectID);
		this.shaderObjectID = 0;
		this.compilationStatus = Status.UNINITIALIZED;
	}
}
