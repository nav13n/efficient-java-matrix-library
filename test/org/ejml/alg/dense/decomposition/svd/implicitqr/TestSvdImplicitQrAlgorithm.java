/*
 * Copyright (c) 2009-2010, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * EJML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * EJML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EJML.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ejml.alg.dense.decomposition.svd.implicitqr;

import org.ejml.UtilEjml;
import org.ejml.alg.dense.decomposition.bidiagonal.BidiagonalDecompositionRow;
import org.ejml.data.DenseMatrix64F;
import org.ejml.data.SimpleMatrix;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestSvdImplicitQrAlgorithm {

    private Random rand = new Random(234234);

    /**
     * Computes the singular values of a bidiagonal matrix that is all ones.
     * From exercise 5.9.45 in Fundamentals of Matrix Computations.
     *
     */
    @Test
    public void oneBidiagonalMatrix() {
        SvdImplicitQrAlgorithm svd = new SvdImplicitQrAlgorithm(true);
        for( int N = 5; N < 10; N++ ) {
            DenseMatrix64F A = CommonOps.identity(N);
            for( int i = 0; i < N-1; i++ ) {
                A.set(i,i+1,1);
            }


            svd.setMatrix(A);

            assertTrue(svd.process());

            for( int i = 0; i < N; i++ ) {
                double val = 2.0*Math.cos( (i+1)*Math.PI/(2.0*N+1.0));

                assertEquals(1,countNumFound(svd,val,1e-8));
            }
        }
    }

    /**
     * A trivial case where all the elements are diagonal.  It should do nothing here.
     */
    @Test
    public void knownDiagonal() {
        DenseMatrix64F A = CommonOps.diag(1,2,3,4,5);

        SvdImplicitQrAlgorithm svd = new SvdImplicitQrAlgorithm(true);
        svd.setMatrix(A);

        assertTrue(svd.process());

        assertEquals(1,countNumFound(svd,5,1e-8));
        assertEquals(1,countNumFound(svd,4,1e-8));
        assertEquals(1,countNumFound(svd,3,1e-8));
        assertEquals(1,countNumFound(svd,2,1e-8));
        assertEquals(1,countNumFound(svd,1,1e-8));
    }

    /**
     * Sees if it handles the case where there is a zero on the diagonal
     */
    @Test
    public void zeroOnDiagonal() {
        DenseMatrix64F A = CommonOps.diag(1,2,3,4,5,6);
        for( int i = 0; i < 5; i++ ) {
            A.set(i,i+1,2);
        }
        A.set(2,2,0);

//        A.print();

        SvdImplicitQrAlgorithm svd = new SvdImplicitQrAlgorithm(false);
        svd.setMatrix(A);

        assertTrue(svd.process());

        assertEquals(1,countNumFound(svd,6.82550,1e-4));
        assertEquals(1,countNumFound(svd,5.31496,1e-4));
        assertEquals(1,countNumFound(svd,3.76347,1e-4));
        assertEquals(1,countNumFound(svd,3.28207,1e-4));
        assertEquals(1,countNumFound(svd,1.49265,1e-4));
        assertEquals(1,countNumFound(svd,0.00000,1e-4));
    }

    @Test
    public void knownCaseSquare() {
        DenseMatrix64F A = UtilEjml.parseMatrix("-3   1   3  -3   0\n" +
                "   2  -4   0  -2   0\n" +
                "   1  -4   4   1  -3\n" +
                "  -1  -3   2   2  -4\n" +
                "  -5   3   1   3   1",5);

//        A.print();

        SvdImplicitQrAlgorithm svd = createHelper(A);

        assertTrue(svd.process());

        assertEquals(1,countNumFound(svd,9.3431,1e-3));
        assertEquals(1,countNumFound(svd,7.4856,1e-3));
        assertEquals(1,countNumFound(svd,4.9653,1e-3));
        assertEquals(1,countNumFound(svd,1.8178,1e-3));
        assertEquals(1,countNumFound(svd,1.6475,1e-3));
    }

    /**
     * This makes sure the U and V matrices are being correctly by the push code.
     */
    @Test
    public void zeroOnDiagonalFull() {
        for( int where = 0; where < 6; where++ ) {
            DenseMatrix64F A = CommonOps.diag(1,2,3,4,5,6);
            for( int i = 0; i < 5; i++ ) {
                A.set(i,i+1,2);
            }
            A.set(where,where,0);

            checkFullDecomposition(6, A);
        }
    }

    /**
     * Decomposes a random matrix and see if the decomposition can reconstruct the original
     *
     */
    @Test
    public void randomMatricesFullDecompose() {

        for( int N = 2; N <= 20; N++ ) {
//            System.out.println("--------------------------------------");
            DenseMatrix64F A = new DenseMatrix64F(N,N);
            A.set(0,0,rand.nextDouble());
            for( int i = 1; i < N; i++ ) {
                A.set(i,i,rand.nextDouble());
                A.set(i-1,i,rand.nextDouble());
            }

            checkFullDecomposition(N, A);
        }
    }

    /**
     * Checks the full decomposing my multiplying the components together and seeing if it
     * gets the original matrix again.
     */
    private void checkFullDecomposition(int n, DenseMatrix64F a) {
//        a.print();

        SvdImplicitQrAlgorithm svd = createHelper(a);
        svd.setFastValues(true);
        assertTrue(svd.process());

//        System.out.println("Value total steps = "+svd.totalSteps);

        svd.setFastValues(false);
        double values[] = svd.diag.clone();
        svd.setMatrix(a);
        svd.setUt(CommonOps.identity(n));
        svd.setVt(CommonOps.identity(n));
        assertTrue(svd.process(values));

//        System.out.println("Vector total steps = "+svd.totalSteps);

        SimpleMatrix Ut = SimpleMatrix.wrap(svd.getUt());
        SimpleMatrix Vt = SimpleMatrix.wrap(svd.getVt());
        SimpleMatrix W = SimpleMatrix.diag(svd.diag);
//
//            Ut.mult(W).mult(V).print();
        SimpleMatrix A_found = Ut.transpose().mult(W).mult(Vt);
//            A_found.print();

        assertTrue(MatrixFeatures.isIdentical(a,A_found.getMatrix(),1e-8));
//            System.out.println();
    }

    public static SvdImplicitQrAlgorithm createHelper(DenseMatrix64F a) {
        BidiagonalDecompositionRow bidiag = new BidiagonalDecompositionRow();
        assertTrue(bidiag.decompose(a.<DenseMatrix64F>copy()));

        SvdImplicitQrAlgorithm helper = new SvdImplicitQrAlgorithm();
        DenseMatrix64F B = bidiag.getB(null,true);

//        SimpleMatrix U = SimpleMatrix.wrap(bidiag.getU(null));
//        SimpleMatrix V = SimpleMatrix.wrap(bidiag.getV(null));
//
//        U.mult(SimpleMatrix.wrap(B)).mult(V.transpose()).print();

        helper.setMatrix(B);
        return helper;
    }

    /**
     * Counts the number of times the specified eigenvalue appears.
     */
    public int countNumFound( SvdImplicitQrAlgorithm alg , double val , double tol ) {
        int total = 0;

        for( int i = 0; i < alg.getNumberOfSingularValues(); i++ ) {
            double a = Math.abs(alg.getSingularValue(i));

            if( Math.abs(a-val) <= tol ) {
                total++;
            }
        }

        return total;
    }
}