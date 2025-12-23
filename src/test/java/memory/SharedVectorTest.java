package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedVectorTest {


    @Test
    void transpose() {
        double[] vector = {1,2,3,4,5};
        SharedVector sharedVector = new SharedVector(vector,VectorOrientation.COLUMN_MAJOR);

        sharedVector.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR,sharedVector.getOrientation(),"transposing from column to row doesn't work");

        sharedVector.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR,sharedVector.getOrientation(),"transposing from column to row doesn't work");
    }

    @Test
    void add() {

        double[] vector1 = {1,-2,-4};
        double[]  vector2= {-5,0,-8};
        SharedVector sharedVector1 = new SharedVector(vector1,VectorOrientation.COLUMN_MAJOR);
        SharedVector sharedVector2 = new SharedVector(vector2,VectorOrientation.COLUMN_MAJOR);
        sharedVector1.add(sharedVector2);

        //result should be on v1
        assertEquals(-4.0,sharedVector1.get(0),"vector addition is wrong");
        assertEquals(-2.0,sharedVector1.get(1),"vector addition is wrong");
        assertEquals(-12.0,sharedVector1.get(2),"vector addition is wrong");

        //v2 should not change
        assertEquals(-5.0,sharedVector2.get(0),"vector addition is changing given vector");
        assertEquals(0.0,sharedVector2.get(1),"vector addition is changing given vector");
        assertEquals(-8.0,sharedVector2.get(2),"vector addition is changing given vector");

    }

    @Test
    void negate() {

        double[] vector = {1,-2,0};
        SharedVector sharedVector = new SharedVector(vector,VectorOrientation.COLUMN_MAJOR);
        sharedVector.negate();

        //every value in v1 should be negated
        assertEquals(-1.0,sharedVector.get(0),"negation doesn't work");
        assertEquals(2.0,sharedVector.get(1),"negation doesn't work");
        assertEquals(-0.0,sharedVector.get(2),"negation doesn't work");

    }

    @Test
    void dot() {

        double[] d1 = {1.0, 2.0, -3.0};
        double[] d2 = {4.0, -5.0, 6.0};
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.COLUMN_MAJOR);

        double result = v1.dot(v2);

        // Expected: (1*4) + (2*(-5)) + (-3*6) = 4  - 10 - 18 = -24
        assertEquals(-24.0, result, "scalar multiplication among vectors is not working");

    }

    @Test
    void vecMatMul() {
        double[] vector = {1.0, 2.0, 3.0};
        SharedVector shardVector = new SharedVector(vector, VectorOrientation.ROW_MAJOR);

        double[][] matrix = {
                {1.0, 2.0}, // Row 0
                {1.0, 2.0}, // Row 1
                {1.0, 2.0}  // Row 2
        };

        SharedMatrix sharedMatrix = new SharedMatrix();
        sharedMatrix.loadColumnMajor(matrix);
        shardVector.vecMatMul(sharedMatrix);

        // Result Length should be 2 (because matrix has 2 columns)
        assertEquals(2, shardVector.length(), "Result should have length equal to matrix columns");


        // Index 0: (1*1) + (2*1) + (3*1) = 6
        assertEquals(6.0, shardVector.get(0), 0.0001);

        // Index 1: (1*2) + (2*2) + (3*2) = 12
        assertEquals(12.0, shardVector.get(1), 0.0001);
    }
}