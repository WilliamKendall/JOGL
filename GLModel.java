import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.VectorUtil;

public class GLModel {
  private GL2 gl;
  private int modelIndex;

  private float[] modelMin = new float[3];
  private float[] modelMax = new float[3];


  GLModel(GL2 gl, File file) {
    this.gl = gl;
    loadPLY(file);
  }

  public void distroy() {
    gl.glDeleteLists(this.modelIndex, 1);
  }


  public float[] getMin() {
    return this.modelMin.clone();
  }

  public float[] getMax() {
    return this.modelMax.clone();
  }


  public void draw() {
    gl.glPushMatrix();
    gl.glCallList(this.modelIndex);
    gl.glPopMatrix();
  }


  public float longestEdge()
  {
     float sizeX,sizeY,sizeZ;
     sizeX = this.modelMax[0]-this.modelMin[0];
     sizeY = this.modelMax[1]-this.modelMin[1];
     sizeZ = this.modelMax[2]-this.modelMin[2];
    
     return sizeX >= sizeY? (sizeX>=sizeZ?sizeX:sizeZ) : (sizeY>=sizeZ? sizeY:sizeZ);
  }


  public void drawBoundingBox() {
    boolean glLighting = gl.glIsEnabled(GL2.GL_LIGHTING);
    if (glLighting)
      gl.glDisable(GL2.GL_LIGHTING);

    gl.glPushMatrix();
    gl.glColor3f(1.0f, 0.0f, 0.0f);

    gl.glBegin(GL2.GL_LINES);

    // long edge
    gl.glVertex3f(modelMin[0], modelMin[1], modelMin[2]);
    gl.glVertex3f(modelMax[0], modelMin[1], modelMin[2]);

    gl.glVertex3f(modelMin[0], modelMin[1], modelMax[2]);
    gl.glVertex3f(modelMax[0], modelMin[1], modelMax[2]);

    gl.glVertex3f(modelMin[0], modelMax[1], modelMin[2]);
    gl.glVertex3f(modelMax[0], modelMax[1], modelMin[2]);

    gl.glVertex3f(modelMin[0], modelMax[1], modelMax[2]);
    gl.glVertex3f(modelMax[0], modelMax[1], modelMax[2]);

    // left side
    gl.glVertex3f(modelMin[0], modelMin[1], modelMin[2]);
    gl.glVertex3f(modelMin[0], modelMin[1], modelMax[2]);

    gl.glVertex3f(modelMin[0], modelMax[1], modelMin[2]);
    gl.glVertex3f(modelMin[0], modelMax[1], modelMax[2]);

    gl.glVertex3f(modelMin[0], modelMin[1], modelMin[2]);
    gl.glVertex3f(modelMin[0], modelMax[1], modelMin[2]);

    gl.glVertex3f(modelMin[0], modelMin[1], modelMax[2]);
    gl.glVertex3f(modelMin[0], modelMax[1], modelMax[2]);

    // right side
    gl.glVertex3f(modelMax[0], modelMin[1], modelMin[2]);
    gl.glVertex3f(modelMax[0], modelMin[1], modelMax[2]);

    gl.glVertex3f(modelMax[0], modelMax[1], modelMin[2]);
    gl.glVertex3f(modelMax[0], modelMax[1], modelMax[2]);

    gl.glVertex3f(modelMax[0], modelMin[1], modelMin[2]);
    gl.glVertex3f(modelMax[0], modelMax[1], modelMin[2]);

    gl.glVertex3f(modelMax[0], modelMin[1], modelMax[2]);
    gl.glVertex3f(modelMax[0], modelMax[1], modelMax[2]);


    gl.glEnd();
    gl.glPopMatrix();

    if (glLighting)
      gl.glEnable(GL2.GL_LIGHTING);
  }

  private void loadPLY(File file) {
    // this is really quick a dirty
    int vertexCount = 0;
    int faceCount = 0;
    float[][] verts = null;
    int[][] faces = null;

    // open file
    try {
      Scanner scan = new Scanner(file);

      if (scan.next().compareToIgnoreCase("ply") != 0) {
        // not a ply file
        System.out.println("PLY invalid");
        scan.close();
        return;
      }


      // read ply
      boolean finished = false;
      while (!finished) {
        switch (scan.next()) {
          case "format": {
            // its ascii i assume
            scan.nextLine();
            break;
          }
          case "comment": {
            scan.nextLine();
            break;
          }

          case "property": {
            // throw away unused properties, like normals. (We should use these)
            scan.nextLine();
            break;
          }

          case "element": {
            String elementType = scan.next();
            if (elementType.compareToIgnoreCase("vertex") == 0) {
              // could be anything, but I am just going to say x,y,z
              vertexCount = scan.nextInt();
              scan.nextLine(); // consume the rest of this line
              scan.nextLine();// property x
              scan.nextLine();// property y
              scan.nextLine();// property z

            } else if (elementType.compareToIgnoreCase("face") == 0) {
              faceCount = scan.nextInt();
              scan.nextLine(); // consume the rest of this line
              scan.nextLine();// assume property list uchar int vertex_indices
            }

            break;

          }
          case "end_header": {
            finished = true;
            break;
          }

        }// end switch
      } // end while

      verts = new float[vertexCount][3];
      faces = new int[faceCount][3];


      // read vertices
      for (int i = 0; i < vertexCount; i++) {
        verts[i][0] = scan.nextFloat();
        verts[i][1] = scan.nextFloat();
        verts[i][2] = scan.nextFloat();
        scan.nextLine(); // read eol

        // object size stuff
        if (i == 0) {
          modelMax[0] = verts[0][0];
          modelMin[0] = verts[0][0];

          modelMax[1] = verts[0][1];
          modelMin[1] = verts[0][1];

          modelMax[2] = verts[0][2];
          modelMin[2] = verts[0][2];
        }

        if (verts[i][0] > modelMax[0])
          modelMax[0] = verts[i][0];
        if (verts[i][0] < modelMin[0])
          modelMin[0] = verts[i][0];

        if (verts[i][1] > modelMax[1])
          modelMax[1] = verts[i][1];
        if (verts[i][1] < modelMin[1])
          modelMin[1] = verts[i][1];

        if (verts[i][2] > modelMax[2])
          modelMax[2] = verts[i][2];
        if (verts[i][2] < modelMin[2])
          modelMin[2] = verts[i][2];
      }



      // read faces
      for (int i = 0; i < faceCount; i++) {
        if (scan.nextInt() != 3)
          System.out.println("polys not triangles");
        faces[i][0] = scan.nextInt();
        faces[i][1] = scan.nextInt();
        faces[i][2] = scan.nextInt();
      }

      scan.close();
    } catch (

    FileNotFoundException e) {

      e.printStackTrace();
    }


    // ask gl for a display list
    int model;
    model = gl.glGenLists(1);
    gl.glNewList(model, GL2.GL_COMPILE);

    float[] u = new float[3];
    float[] v = new float[3];
    float[] normalResult = new float[3];
    // start building
    gl.glBegin(GL2.GL_TRIANGLES);// could be poly maybe?
    for (int i = 0; i < faceCount; i++) {

      VectorUtil.subVec3(u, verts[faces[i][1]], verts[faces[i][0]]);
      VectorUtil.subVec3(v, verts[faces[i][2]], verts[faces[i][0]]);
      normalResult[0] = (u[1] * v[2]) - (u[2] * v[1]);
      normalResult[1] = (u[2] * v[0]) - (u[0] * v[2]);
      normalResult[2] = (u[0] * v[1]) - (u[1] * v[0]);

      gl.glNormal3fv(normalResult, 0);

      gl.glVertex3f(verts[faces[i][0]][0], verts[faces[i][0]][1], verts[faces[i][0]][2]);

      gl.glVertex3f(verts[faces[i][1]][0], verts[faces[i][1]][1], verts[faces[i][1]][2]);

      gl.glVertex3f(verts[faces[i][2]][0], verts[faces[i][2]][1], verts[faces[i][2]][2]);


    }
    gl.glEnd();
    gl.glEndList();

    this.modelIndex = model;
  }

}
