
package com.kye.udaf;

import com.kye.utils.BitmapUtil;
import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.AbstractGenericUDAFResolver;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.BinaryObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.roaringbitmap.longlong.Roaring64Bitmap;

import java.io.IOException;

/**
 * ToBitmap.
 */
@Description(name = "to_bitmap", value = "_FUNC_(expr) - Returns an doris bitmap representation of a column.")
public class ToBitmapUDAF extends AbstractGenericUDAFResolver {

    @Override
    public GenericUDAFEvaluator getEvaluator(TypeInfo[] parameters)
            throws SemanticException {
        if (parameters.length != 1) {
            throw new UDFArgumentTypeException(parameters.length - 1,
                    "Exactly one argument is expected.");
        }
        return new GenericEvaluate();
    }

    //The UDAF evaluator assumes that all rows it's evaluating have
    //the same (desired) value.
    public static class GenericEvaluate extends GenericUDAFEvaluator {

        // For PARTIAL1 and COMPLETE: ObjectInspectors for original data
        private PrimitiveObjectInspector inputOI;

        // For PARTIAL2 and FINAL: ObjectInspectors for partial aggregations
        // (doris bitmaps)

        private transient BinaryObjectInspector internalMergeOI;

        @Override
        public ObjectInspector init(Mode m, ObjectInspector[] parameters)
                throws HiveException {
            super.init(m, parameters);
            // init output object inspectors
            // The output of a partial aggregation is a binary
            if (m == Mode.PARTIAL1) {
                inputOI = (PrimitiveObjectInspector) parameters[0];
            } else {
                this.internalMergeOI = (BinaryObjectInspector) parameters[0];
            }
            return PrimitiveObjectInspectorFactory.javaByteArrayObjectInspector;
        }

        /**
         * class for storing the current partial result aggregation
         */
        @AggregationType(estimable = true)
        static class BitmapAgg extends AbstractAggregationBuffer {
            Roaring64Bitmap bitmap;
        }

        @Override
        public void reset(AggregationBuffer agg) throws HiveException {
            ((BitmapAgg) agg).bitmap = new Roaring64Bitmap();
        }

        @Override
        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            BitmapAgg result = new BitmapAgg();
            reset(result);
            return result;
        }

        @Override
        public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
            assert (parameters.length == 1);
            Object p = parameters[0];
            if (p != null) {
                BitmapAgg myagg = (BitmapAgg) agg;
                try {
                    long row = PrimitiveObjectInspectorUtils.getLong(p, inputOI);
                    addBitmap(row, myagg);
                } catch (NumberFormatException e) {
                    throw new HiveException(e);
                }
            }
        }

        @Override
        public Object terminate(AggregationBuffer agg) {
            BitmapAgg myagg = (BitmapAgg) agg;
            try {
                return BitmapUtil.serializeToBytes(myagg.bitmap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void merge(AggregationBuffer agg, Object partial) {
            BitmapAgg myagg = (BitmapAgg) agg;
            byte[] partialResult = this.internalMergeOI.getPrimitiveJavaObject(partial);
            try {
                myagg.bitmap.or(BitmapUtil.deserializeToBitmap(partialResult));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object terminatePartial(AggregationBuffer agg) {
            return terminate(agg);
        }

        private void addBitmap(long newRow, BitmapAgg myagg) {
            myagg.bitmap.add(newRow);
        }
    }
}