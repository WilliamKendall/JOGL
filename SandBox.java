

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Shows a scene (a teapot on a short cylindrical base) that is illuminated by up to four lights
 * plus global ambient light. The user can turn the lights on and off. The global ambient light is a
 * dim white. There is a white "viewpoint" light that points from the direction of the viewer into
 * the scene. There is a red light, a blue light, and a green light that rotate in circles above the
 * teapot. (The user can turn the animation on and off.) The locations of the colored lights are
 * marked by spheres, which are gray when the light is off and are colored by some emission color
 * when the light is on. The teapot is gray with weak specular highlights. The base is colored with
 * a spectrum. (The user can turn the display of the base on and off.) The mouse can be used to
 * rotate the scene.
 */
public class SandBox extends JPanel implements GLEventListener {

  private static final long serialVersionUID = 1L;

  public static void main(String[] args) {
    JFrame window = new JFrame("A Lighting Demo");
    SandBox panel = new SandBox();
    window.setContentPane(panel);
    window.pack();
    window.setLocation(50, 50);
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setVisible(true);
  }

  private JCheckBox animating; // Checked if animation is running.

  private JCheckBox viewpointLight; // Checked if the white viewpoint light is on.
  private JCheckBox redLight; // Checked if the red light is on.
  private JCheckBox greenLight; // Checked if the green light is on.
  private JCheckBox blueLight; // Checked if the blue light is on.
  private JCheckBox ambientLight; // Checked if the global ambient light is on.

  private JCheckBox drawBase; // Checked if the base should be drawn.

  private JSlider spotCutOff;
  private JSlider spotEx;
  private JSlider spotAt;


  private GLJPanel display;
  private Timer animationTimer;

  private int frameNumber = 0; // The current frame number for an animation.

  private Camera camera;

  private GLUT glut = new GLUT();

  // textures
  Texture brickTex; // wall texture
  int room; // room display list
  GLModel model;
  float modelHeight;

  /**
   * The constructor adds seven checkboxes under the display, to control the options.
   * 
   * @return
   */
  public SandBox() {
    GLCapabilities caps = new GLCapabilities(null);
    display = new GLJPanel(caps);
    display.setPreferredSize(new Dimension(600, 600));
    display.addGLEventListener(this);
    setLayout(new BorderLayout());
    add(display, BorderLayout.CENTER);
    camera = new Camera();
    camera.lookAt(5, 10, 30, 0, 0, 0, 0, 1, 0);
    camera.setScale(15);

    // need to render a little further
    camera.setLimits(-15, 15, -15, 15, -120, 30);

    camera.installTrackball(display);
    animationTimer = new Timer(30, new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        frameNumber++;
        display.repaint();
      }
    });
    ActionListener boxHandler = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == animating) {
          if (animating.isSelected()) {
            animationTimer.start();
          } else {
            animationTimer.stop();
          }
        } else {
          display.repaint();
        }
      }
    };
    viewpointLight = new JCheckBox("Viewpoint Light", false);
    redLight = new JCheckBox("Red Light", false);
    blueLight = new JCheckBox("Blue Light", false);
    greenLight = new JCheckBox("Green Light", false);
    ambientLight = new JCheckBox("Global Ambient Light", false);
    animating = new JCheckBox("Animate", true);
    drawBase = new JCheckBox("Draw Base", false);

    spotCutOff = new JSlider(0, 90);
    spotEx = new JSlider(0, 128);
    spotAt = new JSlider(0, 10);

    viewpointLight.addActionListener(boxHandler);
    ambientLight.addActionListener(boxHandler);
    redLight.addActionListener(boxHandler);
    greenLight.addActionListener(boxHandler);
    blueLight.addActionListener(boxHandler);
    animating.addActionListener(boxHandler);
    drawBase.addActionListener(boxHandler);
    JPanel bottom = new JPanel();
    bottom.setLayout(new GridLayout(5, 1));
    JPanel row1 = new JPanel();
    row1.add(animating);
    row1.add(drawBase);
    row1.add(ambientLight);
    bottom.add(row1);
    JPanel row2 = new JPanel();
    row2.add(viewpointLight);
    row2.add(redLight);
    row2.add(greenLight);
    row2.add(blueLight);
    bottom.add(row2);
    add(bottom, BorderLayout.SOUTH);
    animationTimer.setInitialDelay(500);
    animationTimer.start();
    JPanel row3 = new JPanel();
    row3.add(new JLabel("Spotlight Cutoff Angle"));
    row3.add(spotCutOff);
    bottom.add(row3);
    JPanel row4 = new JPanel();
    row4.add(new JLabel("Spotlight Attenuation"));
    row4.add(spotAt);
    bottom.add(row4);
    JPanel row5 = new JPanel();
    row5.add(new JLabel("Spotlight Exponent"));
    row5.add(spotEx);
    bottom.add(row5);


  }

  // ----------------------------- Methods for drawing -------------------------------

  /**
   * Sets the positions of the colored lights and turns them on and off, depending on the state of
   * the redLight, greenLight, and blueLight options. Draws a small sphere at the location of each
   * light.
   */
  private void lights(GL2 gl) {

    gl.glColor3d(0.5, 0.5, 0.5);
    float zero[] = {0, 0, 0, 1};
    gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, zero, 0);

    if (viewpointLight.isSelected())
      gl.glEnable(GL2.GL_LIGHT0);
    else
      gl.glDisable(GL2.GL_LIGHT0);

    if (redLight.isSelected()) {
      float red[] = {0.5F, 0, 0, 1};
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, red, 0);
      gl.glEnable(GL2.GL_LIGHT1);
    } else {
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, zero, 0);
      gl.glDisable(GL2.GL_LIGHT1);
    }
    gl.glPushMatrix();
    gl.glRotated(-frameNumber, 0, 1, 0);
    gl.glTranslated(10, 7, 0);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, zero, 0);
    glut.glutSolidSphere(0.5, 16, 8);
    gl.glPopMatrix();

    if (greenLight.isSelected()) {
      float green[] = {0, 0.5F, 0, 1};
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, green, 0);
      gl.glEnable(GL2.GL_LIGHT2);
    } else {
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, zero, 0);
      gl.glDisable(GL2.GL_LIGHT2);
    }
    gl.glPushMatrix();
    gl.glRotated((frameNumber + 100) * 0.8743, 0, 1, 0);
    gl.glTranslated(9, 8, 0);
    gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_POSITION, zero, 0);
    glut.glutSolidSphere(0.5, 16, 8);
    gl.glPopMatrix();

    if (blueLight.isSelected()) {
      float blue[] = {0, 0, 0.5F, 1};
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, blue, 0);
      gl.glEnable(GL2.GL_LIGHT3);
    } else {
      gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, zero, 0);
      gl.glDisable(GL2.GL_LIGHT3);
    }
    gl.glPushMatrix();
    gl.glRotated((frameNumber - 100) * 1.3057, 0, 1, 0);
    gl.glTranslated(9.5, 7.5, 0);
    gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_POSITION, zero, 0);
    glut.glutSolidSphere(0.5, 16, 8);
    gl.glPopMatrix();

    gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, zero, 0); // Turn off emission color!
  } // end lights()

  /**
   * Creates an array containing the RGBA corresponding to the specified hue, with saturation 1 and
   * brightness 0.6. The hue should be in the range 0.0 to 1.0.
   */
  private float[] colorArrayForHue(double hue) {
    Color c = Color.getHSBColor((float) hue, 1, 0.6F);
    return new float[] {c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1};
  }



  private void buildRoom(GL2 gl) {

    room = gl.glGenLists(1);
    gl.glNewList(room, GL2.GL_COMPILE);


    // square
    float tes = .1f;
    float tile = 10;
    // bottom
    gl.glPushMatrix();
    gl.glTranslatef(0, -5, 0);
    square(gl, 30, 30, tile, tile, tes, tes);
    gl.glPopMatrix();

    // back
    gl.glPushMatrix();
    gl.glTranslatef(0, 0, -15);
    gl.glRotated(90, 1, 0, 0);
    square(gl, 30, 10, tile, tile, tes, tes);
    gl.glPopMatrix();

    // left
    gl.glPushMatrix();
    gl.glTranslatef(-15, 0, 0);
    gl.glRotated(-90, 0, 0, 1);
    square(gl, 10, 30, tile, tile, tes, tes);
    gl.glPopMatrix();

    // right
    gl.glPushMatrix();
    gl.glTranslatef(15, 0, 0);
    gl.glRotated(90, 0, 0, 1);
    square(gl, 10, 30, tile, tile, tes, tes);
    gl.glPopMatrix();

    // back
    gl.glPushMatrix();
    gl.glTranslatef(0, 0, 15);
    gl.glRotated(-90, 1, 0, 0);
    square(gl, 30, 10, tile, tile, tes, tes);
    gl.glPopMatrix();

    gl.glEndList();
  }

  private void drawRoom(GL2 gl) {

    gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT);
    gl.glEnable(GL2.GL_CULL_FACE);
    gl.glCullFace(GL2.GL_BACK);
    // start texture
    brickTex.bind(gl);
    gl.glEnable(GL2.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_BLEND);
    gl.glBlendFunc(GL2.GL_ONE, GL2.GL_SRC_COLOR);
    brickTex.enable(gl);

    // allow textures to wrap
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_MIRRORED_REPEAT);
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_MIRRORED_REPEAT);


    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[] {0.3F, 0.3F, 0.3F, 1}, 0);
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, new float[] {0.4F, 0.4F, 0.4F, 1}, 0);
    gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 90);


    gl.glCallList(room);

    brickTex.disable(gl);
    gl.glPopAttrib();

  }

  public void square(GL2 gl, float sizeX, float sizeY, float texRepX, float texRepY, float tesX,
      float tesY) {
    gl.glTranslatef(-1 * (sizeX / 2), 0, -1 * (sizeY / 2));// center
    gl.glBegin(GL2.GL_QUADS);
    float u, v, tx, ty;
    tx = 1.0f / (sizeX / tesX) * texRepX;
    ty = 1.0f / (sizeY / tesY) * texRepY;
    for (float x = 0; x < sizeX; x += tesX) {
      for (float z = 0; z < sizeY; z += tesY) {
        u = (1.0f / (sizeX / tesX)) * (x / tesX) * texRepX;
        v = (1.0f / (sizeY / tesY)) * (z / tesY) * texRepY;

        gl.glNormal3f(0, 1, 0);
        gl.glTexCoord2f(u + tx, v);
        gl.glVertex3d(x + tesX, 0, z); // back right

        gl.glNormal3f(0, 1, 0);
        gl.glTexCoord2f(u, v);
        gl.glVertex3d(x, 0, z);// back left

        gl.glNormal3f(0, 1, 0);
        gl.glTexCoord2f(u, v + ty);
        gl.glVertex3d(x, 0, z + tesY); // front left

        gl.glNormal3f(0, 1, 0);
        gl.glTexCoord2f(u + tx, v + ty);
        gl.glVertex3d(x + tesX, 0, z + tesY); // front right
      }
    }
    gl.glEnd();

  }

  /**
   * Draws a cylinder with height 2 and radius 1, centered at the origin, with its axis along the
   * z-axis. A spectrum of hues is applied to the vertices along the edges of the cylinder. (Since
   * GL_COLOR_MATERIAL is enabled in this program, the colors specified here are used as ambient and
   * diffuse material colors for the cylinder.)
   */
  private void drawCylinder(GL2 gl) {
    gl.glBegin(GL2.GL_TRIANGLE_STRIP);
    for (int i = 0; i <= 64; i++) {
      double angle = 2 * Math.PI / 64 * i;
      double x = Math.cos(angle);
      double y = Math.sin(angle);
      gl.glColor3fv(colorArrayForHue(i / 64.0), 0); // sets ambient and diffuse material
      gl.glNormal3d(x, y, 0); // Normal for both vertices at this angle.
      gl.glVertex3d(x, y, 1); // Vertex on the top edge.
      gl.glVertex3d(x, y, -1); // Vertex on the bottom edge.
    }
    gl.glEnd();
    gl.glNormal3d(0, 0, 1);
    gl.glBegin(GL2.GL_TRIANGLE_FAN); // Draw the top, in the plane z = 1.
    gl.glColor3d(1, 1, 1); // ambient and diffuse for center
    gl.glVertex3d(0, 0, 1);
    for (int i = 0; i <= 64; i++) {
      double angle = 2 * Math.PI / 64 * i;
      double x = Math.cos(angle);
      double y = Math.sin(angle);
      gl.glColor3fv(colorArrayForHue(i / 64.0), 0);
      gl.glVertex3d(x, y, 1);
    }
    gl.glEnd();
    gl.glNormal3f(0, 0, -1);
    gl.glBegin(GL2.GL_TRIANGLE_FAN); // Draw the bottom, in the plane z = -1
    gl.glColor3d(1, 1, 1); // ambient and diffuse for center
    gl.glVertex3d(0, 0, -1);
    for (int i = 64; i >= 0; i--) {
      double angle = 2 * Math.PI / 64 * i;
      double x = Math.cos(angle);
      double y = Math.sin(angle);
      gl.glColor3fv(colorArrayForHue(i / 64.0), 0);
      gl.glVertex3d(x, y, -1);
    }
    gl.glEnd();
  }

  // --------------- Methods of the GLEventListener interface -----------

  /**
   * Draws the scene.
   */
  public void display(GLAutoDrawable drawable) {
    // called when the panel needs to be drawn

    GL2 gl = drawable.getGL().getGL2();



    gl.glClearColor(0, 0, 0, 0);
    gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    camera.apply(gl);


    // light stuff
    lights(gl);



    float zero[] = {0, 0, 0, 1};

    if (ambientLight.isSelected()) {
      gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[] {0.15F, 0.15F, 0.15F, 1}, 0);
    } else {
      gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, zero, 0);
    }



    if (drawBase.isSelected()) {
      gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, zero, 0);
      gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[] {1.0F, 1.0F, 1.0F, 1}, 0);

      gl.glPushMatrix();
      gl.glTranslated(0, -5, 0);
      gl.glRotated(-90, 1, 0, 0);
      gl.glScaled(10, 10, 0.5);
      drawCylinder(gl);
      gl.glPopMatrix();
    }



    // Draw room
    // spotlight


    float spot[] = {0, 0, 0, 1};
    float look[] = {0, -1f, 0};
    gl.glPushMatrix();
    gl.glTranslated(0, 25, 0);
    gl.glLightf(GL2.GL_LIGHT4, GL2.GL_SPOT_CUTOFF, (float) spotCutOff.getValue());
    gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_POSITION, spot, 0);
    gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_SPOT_DIRECTION, look, 0);
    gl.glLightf(GL2.GL_LIGHT4, GL2.GL_CONSTANT_ATTENUATION, (float) spotAt.getValue());
    gl.glLightf(GL2.GL_LIGHT4, GL2.GL_SPOT_EXPONENT, (float) spotEx.getValue());
    gl.glEnable(GL2.GL_LIGHT4);
    gl.glPopMatrix();


    // dragon
    float floor = -5.5f;
    float dragonY = (((float) frameNumber) / 60.0f) % 10.0f;
    dragonY = 0;// dragonY>5? 5-(((((float)frameNumber)/60.0f) %
                // 10.0f)-5):(((float)frameNumber)/60.0f) % 10.0f;



    gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT);
    gl.glEnable(GL2.GL_CULL_FACE);
    gl.glCullFace(GL2.GL_BACK);

    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, new float[] {5F, 5F, 5F, 1}, 0);
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[] {0.3F, 1.0F, 0.3F, 1}, 0);
    gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 90);

    gl.glPushMatrix();


    // gl.glTranslatef(0, -((modelHeight*50)/2)+1+dragonY, 0);
    // gl.glTranslatef(0,floor, 0);

    gl.glScalef(1 / model.longestEdge(), 1 / model.longestEdge(), 1 / model.longestEdge());
    gl.glScalef(10, 10, 10);
    gl.glTranslatef(-model.getMin()[0] - ((model.getMax()[0] - model.getMin()[0]) / 2), 0, 0);
    gl.glTranslatef(0, -model.getMin()[1] - ((model.getMax()[1] - model.getMin()[1]) / 2), 0);
    gl.glTranslatef(0, 0, -model.getMin()[2] - ((model.getMax()[2] - model.getMin()[2]) / 2));


    // gl.glTranslated(0, floor, 0);

    model.drawBoundingBox();
    model.draw();
    // model.drawNormalized();

    gl.glPopMatrix();
    gl.glDisable(GL2.GL_CULL_FACE);
    gl.glPopAttrib();


    gl.glEnable(GL2.GL_STENCIL_TEST);

    // Draw floor
    gl.glStencilFunc(GL2.GL_ALWAYS, 1, 1); // Set any stencil to 1
    gl.glStencilOp(GL2.GL_KEEP, GL2.GL_KEEP, GL2.GL_REPLACE);

    gl.glStencilMask(1); // Write to stencil buffer
    gl.glDepthMask(false); // Don't write to depth buffer
    gl.glClear(GL2.GL_STENCIL_BUFFER_BIT); // Clear stencil buffer (0 by default)

    // room
    gl.glMaterialfv(GL2.GL_BACK, GL2.GL_SPECULAR | GL2.GL_AMBIENT_AND_DIFFUSE, zero, 0);
    gl.glPushMatrix();
    gl.glTranslated(0, -0.5f, 0);
    drawRoom(gl); // really draw the room
    gl.glPopMatrix();

    // end room

    // Draw dragon reflection
    gl.glStencilFunc(GL2.GL_EQUAL, 1, 1); // Pass test if stencil value is 1
    gl.glStencilMask(0); // Don't write anything to stencil buffer
    gl.glDepthMask(true); // Write to depth buffer
    gl.glStencilOp(GL2.GL_KEEP, GL2.GL_KEEP, GL2.GL_KEEP);

    // dragon
    gl.glPushAttrib(GL2.GL_LIGHTING_BIT | GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT);
    gl.glEnable(GL2.GL_CULL_FACE);
    gl.glCullFace(GL2.GL_BACK);

    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, new float[] {5F, 5F, 5F, 1}, 0);
    gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, new float[] {0.3F, 1.0F, 0.3F, 1}, 0);
    gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 90);

    gl.glPushMatrix();

    gl.glScalef(1, -1, 1);

    gl.glTranslatef(0, -((modelHeight * 50) / 2) + 1 + dragonY, 0);
    gl.glTranslatef(0, -floor, 0);

    gl.glScalef(50, 50, 50);

    // model.draw();
    gl.glPopMatrix();
    gl.glDisable(GL2.GL_CULL_FACE);
    gl.glPopAttrib();


    gl.glDisable(GL2.GL_STENCIL_TEST);

  }

  /**
   * Initialization, including setting up a camera and configuring the four lights.
   */
  public void init(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glClearColor(0, 0, 0, 1);
    gl.glEnable(GL2.GL_DEPTH_TEST);
    gl.glEnable(GL2.GL_LIGHTING);
    gl.glEnable(GL2.GL_LIGHT0);
    gl.glEnable(GL2.GL_NORMALIZE);
    // gl.glEnable(GL2.GL_COLOR_MATERIAL);
    gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, 1);
    gl.glMateriali(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 32);

    float dim[] = {0.5F, 0.5F, 0.5F, 1};
    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, dim, 0);
    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, dim, 0);

    float red[] = {0.5F, 0, 0, 1};
    float reda[] = {0.1F, 0, 0, 1};
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, reda, 0);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, red, 0);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, red, 0);

    float gr[] = {0, 0.5F, 0, 1};
    float gra[] = {0, 0.1F, 0, 1};
    gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_AMBIENT, gra, 0);
    gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_DIFFUSE, gr, 0);
    gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_SPECULAR, gr, 0);

    float bl[] = {0, 0, 0.5F, 1};
    float bla[] = {0, 0, 0.1F, 1};
    gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_AMBIENT, bla, 0);
    gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_DIFFUSE, bl, 0);
    gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_SPECULAR, bl, 0);


    // spot light
    float wl[] = {4.0f, 4.0f, 4.0f, 1};
    float wla[] = {0.5f, 0.5f, 0.5f, 1};
    gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_AMBIENT, wla, 0);
    gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_DIFFUSE, wl, 0);
    gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_SPECULAR, wl, 0);



    // textures
    File fBrick = new File("brick.png");
    try {
      brickTex = TextureIO.newTexture(fBrick, false);
    } catch (GLException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    brickTex.bind(gl);
    // gl.glGenerateMipmap(GL2.GL_TEXTURE_2D);
    brickTex.disable(gl);

    // build room
    buildRoom(gl);

    model = new GLModel(gl, new File("dragon.ply"));
  }

  /**
   * Called when the size of the GLJPanel changes.
   */
  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}

  /**
   * This is called before the GLJPanel is destroyed.
   */
  public void dispose(GLAutoDrawable drawable) {}



}
