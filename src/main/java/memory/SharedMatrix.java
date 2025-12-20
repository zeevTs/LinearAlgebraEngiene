package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        for(int i=0;i<matrix.length;i++){
            vectors[i] = new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        for(int i=0;i<matrix.length;i++){
            vectors[i] = new SharedVector(matrix[i],VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        for(int i=0;i<matrix.length;i++){
            vectors[i] = new SharedVector(matrix[i],VectorOrientation.COLUMN_MAJOR);
        }
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        double[][] result = new double[vectors.length][vectors[0].length()];
        if(getOrientation() == VectorOrientation.COLUMN_MAJOR){
            for(int i=0;i<vectors.length;i++){
                for(int j=0;i< vectors[i].length();i++){
                    result[i][j] = vectors[j].get(i);
                }
            }
        }else {
            for (int i = 0; i < vectors.length; i++) {
                for (int j = 0; i < vectors[i].length(); i++) {
                    result[i][j] = vectors[i].get(j);
                }
            }
        }
        return result;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for(int i=0;i<length();i++){
            vectors[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for(int i=0;i<length();i++){
            vectors[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for(int i=0;i<length();i++){
            vectors[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for(int i=0;i<length();i++){
            vectors[i].writeUnlock();
        }
    }
}
