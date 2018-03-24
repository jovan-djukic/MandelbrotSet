package shaders;

import com.jogamp.opengl.GL4;

public class FragmentShader extends Shader {
	public FragmentShader(String shaderName) {
		super(GL4.GL_FRAGMENT_SHADER, shaderName);
	}
}