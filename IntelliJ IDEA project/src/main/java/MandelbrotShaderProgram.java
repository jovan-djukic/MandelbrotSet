import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.jogamp.opengl.GL4;

import program.ShaderProgram;
import shaders.FragmentShader;
import shaders.VertexShader;

public class MandelbrotShaderProgram extends ShaderProgram {

	private static class Constants {
		public static final String	vertexShader	= "vertexShader.glsl";
		public static final String	fragmentShader	= "fragmentShader.glsl";
	}

	public static class Uniforms {
		public static final String	maxIteration	= "maxIteration";
		public static final String	maxValue		= "maxValue";
		public static final String	transform		= "transform";
	}

	public MandelbrotShaderProgram(String name) {
		super(name);
		try (
			InputStream vertexShaderInputStream = MandelbrotShaderProgram.class.getResourceAsStream(Constants.vertexShader);
			InputStream fragmentShaderInputStream = MandelbrotShaderProgram.class.getResourceAsStream(Constants.fragmentShader)
		) {
			VertexShader vertexShader = new VertexShader(Constants.vertexShader);
			FragmentShader fragmentShader = new FragmentShader(Constants.fragmentShader);

			vertexShader.setShaderSource(new String(vertexShaderInputStream.readAllBytes()));
			fragmentShader.setShaderSource(new String(fragmentShaderInputStream.readAllBytes()));
			
			super.addShader(vertexShader)
			.addShader(fragmentShader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void build(GL4 gl) {
		ArrayList<String> uniforms = new ArrayList<>();
		
		uniforms.add(Uniforms.maxIteration);
		uniforms.add(Uniforms.maxValue);
		uniforms.add(Uniforms.transform);

		super.build(gl, uniforms);
	}
}
