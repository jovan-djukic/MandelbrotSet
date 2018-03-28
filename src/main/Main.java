package main;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;

import program.ShaderProgram;

public class Main implements GLEventListener, MouseListener {
	private static class Constants {
		public static final String  windowTitle     = "Mandelbrot Set";
		public static final int     alphaBits       = 8;
		public static final int     depthBits       = 24;
		public static final boolean doubleBuffered  = true;
		public static final int     fps             = 60;
		public static final int     windowWidth     = 800;
		public static final int     windowHeight    = 800;
		public static final String  shaderProgram   = "shaderProgram";
		public static final float   startIteration  = 360;
		public static final float   maxValue        = 2;
		public static final int     rotationField   = 1;
		public static final float   mouseWheelUp    = -1;
		public static final float   mouseWheelDown  = 1;
		public static final int     matrixSize      = 16;
		public static final float   startScale      = 1;
		public static final float   scaleUpFactor   = 1.1f;
		public static final float   scaleDownFactor = 0.9f;
	}
	
	private static class FPSAnimatorStopper implements Runnable {
		private FPSAnimator fpsAnimator;

		public FPSAnimatorStopper(FPSAnimator fpsAnimator) {
			this.fpsAnimator = fpsAnimator;
		}

		@Override
		public void run() {
			if (this.fpsAnimator != null) {
				this.fpsAnimator.stop();
			}
		}
	}

	private GLWindow glWindow;
	private FPSAnimator fpsAnimator;

	private ShaderProgram shaderProgram;
	private int vertexArrayObjectID, vertexBufferID, elementBufferID;
	
	private float side, width, height;
	private Matrix4f finalTransform, windowScaleTransform, windowTranslateTransform, userTransform;
	private float[] transformArray;
	private float maxIteration, oldX, oldY;
	
	protected Main(String windowTitle, int alphaBits, int depthBits, boolean doubleBuffer, int fps, int windowWidth, int windowHeight) {
		this.windowScaleTransform = new Matrix4f();
		this.windowScaleTransform.identity();
		
		this.windowTranslateTransform = new Matrix4f();
		this.windowTranslateTransform.identity();
		
		this.finalTransform = new Matrix4f();
		this.finalTransform.identity();
		
		this.userTransform = new Matrix4f();
		this.userTransform.identity();
		
		this.maxIteration = Constants.startIteration;
		this.transformArray = new float[Constants.matrixSize];
		
		GLProfile glProfile = GLProfile.getMaximum(true);
		System.out.println(glProfile.toString());
		System.out.println(glProfile.getGLImplBaseClassName());
		System.out.println(glProfile.getImplName());
		System.out.println(glProfile.getName());
		System.out.println(glProfile.hasGLSL());

		GLCapabilities glCapabilities = new GLCapabilities(glProfile);
		glCapabilities.setAlphaBits(alphaBits);
		glCapabilities.setDepthBits(depthBits);
		glCapabilities.setDoubleBuffered(doubleBuffer);

		this.glWindow = GLWindow.create(glCapabilities);
		this.fpsAnimator = new FPSAnimator(this.glWindow, fps, true);

		this.glWindow.addGLEventListener(this);
		this.glWindow.setWindowDestroyNotifyAction(new FPSAnimatorStopper(this.fpsAnimator));
		this.glWindow.addMouseListener(this);
		this.glWindow.setSize(windowWidth, windowHeight);
		this.glWindow.setTitle(windowTitle);
		this.glWindow.setVisible(true);

		this.fpsAnimator.start();
	}	
	
	protected Main() {
		this(Constants.windowTitle, Constants.alphaBits, Constants.depthBits, Constants.doubleBuffered, Constants.fps, Constants.windowWidth, Constants.windowHeight);
	}
	
	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
		GL4 gl = glAutoDrawable.getGL().getGL4();
		
		gl.glViewport(x, y, width, height);
		
		this.width = width;
		this.height = height;
		this.side = (this.width > this.height) ? (this.height / 2) : (this.width / 2);
		
		this.windowScaleTransform.identity()
				.scale(1 / this.side, 1 / this.side, 1);
		
		this.windowTranslateTransform.identity()
				.translate(-this.width / 2, -this.height / 2, 0);
	}
	
	
	@Override
	public void init(GLAutoDrawable glAutoDrawable) {
		GL4 gl = glAutoDrawable.getGL().getGL4();
		
		this.shaderProgram = new MandelbrotShaderProgram(Constants.shaderProgram);

		this.shaderProgram.build(gl);

		System.out.println(this.shaderProgram.getBuildLog());

		gl.glUseProgram(this.shaderProgram.getProgramID());
		
		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGenVertexArrays(1, intBuffer);

		this.vertexArrayObjectID = intBuffer.get(0);
		gl.glBindVertexArray(this.vertexArrayObjectID);

		float[] vertices = { 
				+1, +1, 0,
				+1, -1, 0,
				-1, -1, 0,
				-1, +1, 0
		};

		FloatBuffer verticesBuffer = Buffers.newDirectFloatBuffer(vertices);

		intBuffer.rewind();
		gl.glGenBuffers(1, intBuffer);
		this.vertexBufferID = intBuffer.get(0);

		gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, this.vertexBufferID);
		gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, verticesBuffer, GL4.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		int[] indices = { 
				0, 1, 2,
				0, 2, 3
		};

		IntBuffer indicesBuffer = Buffers.newDirectIntBuffer(indices);

		intBuffer.rewind();
		gl.glGenBuffers(1, intBuffer);
		this.elementBufferID = intBuffer.get(0);

		gl.glBindBuffer(GL4.GL_ELEMENT_ARRAY_BUFFER, this.elementBufferID);
		gl.glBufferData(GL4.GL_ELEMENT_ARRAY_BUFFER, indices.length * Integer.BYTES, indicesBuffer, GL4.GL_STATIC_DRAW);
	}
	
	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {
		GL4 gl = glAutoDrawable.getGL().getGL4();
		
		gl.glDeleteBuffers(2, new int[] { this.elementBufferID, this.vertexBufferID}, 0);
		gl.glDeleteVertexArrays(1, new int[] {this.vertexArrayObjectID}, 1);
		this.shaderProgram.delete(gl);
	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		GL4 gl = glAutoDrawable.getGL().getGL4();
		
		gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
		
		int maxIterationPosition = this.shaderProgram.getUniforLocation(MandelbrotShaderProgram.Uniforms.maxIteration);
		int maxValuePosition = this.shaderProgram.getUniforLocation(MandelbrotShaderProgram.Uniforms.maxValue);
		int transformPosition = this.shaderProgram.getUniforLocation(MandelbrotShaderProgram.Uniforms.transform);
		
		this.finalTransform.identity()
				.mul(this.windowScaleTransform)
				.mul(this.userTransform)
				.mul(this.windowTranslateTransform)
				.get(this.transformArray);
		
		gl.glUniform1f(maxIterationPosition, this.maxIteration);
		gl.glUniform1f(maxValuePosition, Constants.maxValue);
		gl.glUniformMatrix4fv(transformPosition, 1, false, this.transformArray, 0);
		
		gl.glDrawElements(GL4.GL_TRIANGLES, 6, GL4.GL_UNSIGNED_INT, 0);
		
		int glErrorCode = gl.glGetError();
		if (glErrorCode != GL4.GL_NO_ERROR) {
			System.out.println("ERROR");
			System.out.println(glErrorCode);
			switch (glErrorCode) {
				case GL4.GL_INVALID_VALUE: {
					System.out.println("INVALID VALUE");
				}
				case GL4.GL_INVALID_OPERATION: {
					System.out.println("INVALID OPERATION");
				}
			}
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }

	@Override
	public void mousePressed(MouseEvent e) { 
		this.oldX = e.getX();
		this.oldY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseMoved(MouseEvent e) { }

	@Override
	public void mouseDragged(MouseEvent e) { 
		float shiftX = -1 * (e.getX() - this.oldX);
		float shiftY = e.getY() - this.oldY;
		
		this.oldX = e.getX();
		this.oldY = e.getY();
		
		this.userTransform.translate(shiftX, shiftY, 0);
	}

	@Override
	public void mouseWheelMoved(MouseEvent e) {
		System.out.println(e.getX());
		System.out.println(e.getY());
		
		if (e.getRotation()[Constants.rotationField] == Constants.mouseWheelUp) {
			if (e.isControlDown()) {
				this.maxIteration++;
			} else {
				float cursorX = e.getX() - this.width / 2;
				float cursorY = this.height / 2 - e.getY();
				
				this.userTransform.translate(cursorX, cursorY, 0)
						.scale(Constants.scaleUpFactor, Constants.scaleUpFactor, 1)
						.translate(-cursorX, -cursorY, 0);
			}
		} else if (e.getRotation()[Constants.rotationField] == Constants.mouseWheelDown) {
			if (e.isControlDown()) {
				if (this.maxIteration > 1) {
					this.maxIteration--;
				}
			} else {
				float cursorX = e.getX() - this.width / 2;
				float cursorY = this.height / 2 - e.getY();
				
				this.userTransform.translate(cursorX, cursorY, 0)
						.scale(Constants.scaleDownFactor, Constants.scaleDownFactor, 1)
						.translate(-cursorX, -cursorY, 0);
			}
		}
	}

	public static void main(String[] args) { 
		new Main();
	}
}

