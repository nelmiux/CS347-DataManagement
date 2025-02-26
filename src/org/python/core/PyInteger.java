/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

import org.python.core.stringlib.InternalFormatSpec;
import org.python.core.stringlib.InternalFormatSpecParser;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python int.
 */
@ExposedType(name = "int", doc = BuiltinDocs.int_doc)
public class PyInteger extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyInteger.class);

    /** The minimum value of an int represented by a BigInteger */
    public static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);

    /** The maximum value of an int represented by a BigInteger */
    public static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);

    /** @deprecated Use MIN_INT instead. */
    @Deprecated
    public static final BigInteger minInt = MIN_INT;
    /** @deprecated Use MAX_INT instead. */
    @Deprecated
    public static final BigInteger maxInt = MAX_INT;

    private final int value;

    public PyInteger(PyType subType, int v) {
        super(subType);
        value = v;
    }

    public PyInteger(int v) {
        this(TYPE, v);
    }

    @ExposedNew
    public static PyObject int_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        ArgParser ap = new ArgParser("int", args, keywords, new String[] {"x", "base"}, 0);
        PyObject x = ap.getPyObject(0, null);
        int base = ap.getInt(1, -909);

        if (new_.for_type == subtype) { // A substantive PyInteger is required as the return value

            if (x == null) {
                return Py.Zero;

            } else if (base == -909) {
                if (x instanceof PyBoolean) {
                    return (coerce(x) == 0) ? Py.Zero : Py.One;
                } else if (x instanceof PyByteArray) {
                    // Make use of the string to int conversion in PyString
                    PyString xs = new PyString(x.asString());
                    return asPyInteger(xs);
                } else {
                    return asPyInteger(x);
                }
            } else if (!(x instanceof PyString)) {
                throw Py.TypeError("int: can't convert non-string with explicit base");
            }

            try {
                return Py.newInteger(((PyString)x).atoi(base));
            } catch (PyException pye) {
                if (pye.match(Py.OverflowError)) {
                    return ((PyString)x).atol(base);
                }
                throw pye;
            }

        } else { // A PyIntegerDerived(subtype, ... ) is required as the return value

            if (x == null) {
                return new PyIntegerDerived(subtype, 0);
            } else if (base == -909) {
                PyObject intOrLong = asPyInteger(x);

                if (intOrLong instanceof PyInteger) {
                    return new PyIntegerDerived(subtype, ((PyInteger)intOrLong).getValue());
                } else {
                    throw Py.OverflowError("long int too large to convert to int");
                }

            } else if (!(x instanceof PyString)) {
                throw Py.TypeError("int: can't convert non-string with explicit base");
            }

            return new PyIntegerDerived(subtype, ((PyString)x).atoi(base));
        }
    } // xxx

    /**
     * Convert all sorts of object types to either <code>PyInteger</code> or <code>PyLong</code>,
     * using their {@link PyObject#__int__()} method, whether exposed or not, or if that raises an
     * exception (as the base <code>PyObject</code> one does), using any <code>__trunc__()</code>
     * the type may have exposed. If all this fails, this method raises an exception. Equivalent to CPython
     * <code>PyNumber_Int()</code>.
     * 
     * @param x to convert to an int
     * @return int or long result.
     * @throws PyException (TypeError) if no method of conversion can be found
     * @throws PyException (AttributeError) if neither __int__ nor __trunc__ found (?)
     */
    private static PyObject asPyInteger(PyObject x) throws PyException {
        // XXX: Not sure that this perfectly matches CPython semantics.
        try {
            // Try the object itself (raises AttributeError if not overridden from PyObject)
            return x.__int__();

        } catch (PyException pye) {
            if (!pye.match(Py.AttributeError)) {
                // x had an __int__ method, but something else went wrong: pass it on
                throw pye;

            } else {
                // x did not have an __int__ method, but maybe __trunc__ will work
                try {
                    PyObject integral = x.invoke("__trunc__");
                    return convertIntegralToInt(integral);

                } catch (PyException pye2) {
                    if (!pye2.match(Py.AttributeError)) {
                        throw pye2;
                    }
                    String fmt = "int() argument must be a string or a number, not '%.200s'";
                    throw Py.TypeError(String.format(fmt, x));
                }
            }
        }
    }

    /**
     * Helper called on whatever exposed method <code>__trunc__</code> returned: it may be
     * <code>int</code>, <code>long</code> or something with an exposed <code>__int__</code>.
     * 
     * @return convert to an int.
     * @throws TypeError and AttributeError.
     */
    private static PyObject convertIntegralToInt(PyObject integral) {
        if (!(integral instanceof PyInteger) && !(integral instanceof PyLong)) {
            PyObject i = integral.invoke("__int__");
            if (!(i instanceof PyInteger) && !(i instanceof PyLong)) {
                throw Py.TypeError(String.format("__trunc__ returned non-Integral (type %.200s)",
                                                 integral.getType().fastGetName()));
            }
            return i;
        }
        return integral;
    }

    @ExposedGet(name = "real", doc = BuiltinDocs.int_real_doc)
    public PyObject getReal() {
        return int___int__();
    }

    @ExposedGet(name = "imag", doc = BuiltinDocs.int_imag_doc)
    public PyObject getImag() {
        return Py.newInteger(0);
    }

    @ExposedGet(name = "numerator", doc = BuiltinDocs.int_numerator_doc)
    public PyObject getNumerator() {
        return int___int__();
    }

    @ExposedGet(name = "denominator", doc = BuiltinDocs.int_denominator_doc)
    public PyObject getDenominator() {
        return Py.newInteger(1);
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return int_toString();
    }

    // XXX: need separate __doc__ for __repr__
    @ExposedMethod(names = {"__str__", "__repr__"}, doc = BuiltinDocs.int___str___doc)
    final String int_toString() {
        return Integer.toString(getValue());
    }

    @Override
    public int hashCode() {
        return int_hashCode();
    }

    @ExposedMethod(names = "__hash__", doc = BuiltinDocs.int___hash___doc)
    final int int_hashCode() {
        return getValue();
    }

    @Override
    public boolean __nonzero__() {
        return int___nonzero__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___nonzero___doc)
    final boolean int___nonzero__() {
        return getValue() != 0;
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c == Integer.TYPE || c == Number.class || c == Object.class || c == Integer.class
            || c == Serializable.class) {
            return new Integer(getValue());
        }

        if (c == Boolean.TYPE || c == Boolean.class) {
            return new Boolean(getValue() != 0);
        }
        if (c == Byte.TYPE || c == Byte.class) {
            return new Byte((byte) getValue());
        }
        if (c == Short.TYPE || c == Short.class) {
            return new Short((short) getValue());
        }

        if (c == Long.TYPE || c == Long.class) {
            return new Long(getValue());
        }
        if (c == Float.TYPE || c == Float.class) {
            return new Float(getValue());
        }
        if (c == Double.TYPE || c == Double.class) {
            return new Double(getValue());
        }
        return super.__tojava__(c);
    }

    @Override
    public int __cmp__(PyObject other) {
        return int___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP, doc = BuiltinDocs.int___cmp___doc)
    final int int___cmp__(PyObject other) {
        if (!canCoerce(other)) {
            return -2;
        }
        int v = coerce(other);
        return getValue() < v ? -1 : getValue() > v ? 1 : 0;
    }

    @Override
    public Object __coerce_ex__(PyObject other) {
        return int___coerce_ex__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.int___coerce___doc)
    final PyObject int___coerce__(PyObject other) {
        return adaptToCoerceTuple(int___coerce_ex__(other));
    }

    /**
     * Coercion logic for int. Implemented as a final method to avoid
     * invocation of virtual methods from the exposed coerced.
     */
    final Object int___coerce_ex__(PyObject other) {
        return other instanceof PyInteger ? other : Py.None;
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyInteger;
    }

    private static final int coerce(PyObject other) {
        if (other instanceof PyInteger) {
            return ((PyInteger) other).getValue();
        }
        throw Py.TypeError("xxx");
    }

    @Override
    public PyObject __add__(PyObject right) {
        return int___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___add___doc)
    final PyObject int___add__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerce(right);
        int a = getValue();
        int b = rightv;
        int x = a + b;
        if ((x ^ a) >= 0 || (x ^ b) >= 0) {
            return Py.newInteger(x);
        }
        return new PyLong((long) a + (long) b);
    }

    @Override
    public PyObject __radd__(PyObject left) {
        return int___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___radd___doc)
    final PyObject int___radd__(PyObject left) {
        return __add__(left);
    }

    private static PyObject _sub(int a, int b) {
        int x = a - b;
        if ((x ^ a) >= 0 || (x ^ ~b) >= 0) {
            return Py.newInteger(x);
        }
        return new PyLong((long) a - (long) b);
    }

    @Override
    public PyObject __sub__(PyObject right) {
        return int___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___sub___doc)
    final PyObject int___sub__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return _sub(getValue(), coerce(right));
    }

    @Override
    public PyObject __rsub__(PyObject left) {
        return int___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rsub___doc)
    final PyObject int___rsub__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return _sub(coerce(left), getValue());
    }

    @Override
    public PyObject __mul__(PyObject right) {
        return int___mul__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___mul___doc)
    final PyObject int___mul__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerce(right);

        double x = getValue();
        x *= rightv;

        if (x <= Integer.MAX_VALUE && x >= Integer.MIN_VALUE) {
            return Py.newInteger((int) x);
        }
        return __long__().__mul__(right);
    }

    @Override
    public PyObject __rmul__(PyObject left) {
        return int___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rmul___doc)
    final PyObject int___rmul__(PyObject left) {
        return __mul__(left);
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private static long divide(long x, long y) {
        if (y == 0) {
            throw Py.ZeroDivisionError("integer division or modulo by zero");
        }
        long xdivy = x / y;
        long xmody = x - xdivy * y;

        // If the signs of x and y differ, and the remainder is non-0, C89 doesn't define
        // whether xdivy is now the floor or the ceiling of the infinitely precise
        // quotient.  We want the floor, and we have it iff the remainder's sign matches
        // y's.
        if (xmody != 0 && ((y < 0 && xmody > 0) || (y > 0 && xmody < 0))) {
            xmody += y;
            --xdivy;
            //assert(xmody && ((y ^ xmody) >= 0));
        }
        return xdivy;
    }

    @Override
    public PyObject __div__(PyObject right) {
        return int___div__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___div___doc)
    final PyObject int___div__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        if (Options.division_warning > 0) {
            Py.warning(Py.DeprecationWarning, "classic int division");
        }
        return Py.newInteger(divide(getValue(), coerce(right)));
    }

    @Override
    public PyObject __rdiv__(PyObject left) {
        return int___rdiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rdiv___doc)
    final PyObject int___rdiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        if (Options.division_warning > 0) {
            Py.warning(Py.DeprecationWarning, "classic int division");
        }
        return Py.newInteger(divide(coerce(left), getValue()));
    }

    @Override
    public PyObject __floordiv__(PyObject right) {
        return int___floordiv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___floordiv___doc)
    final PyObject int___floordiv__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newInteger(divide(getValue(), coerce(right)));
    }

    @Override
    public PyObject __rfloordiv__(PyObject left) {
        return int___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rfloordiv___doc)
    final PyObject int___rfloordiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newInteger(divide(coerce(left), getValue()));
    }

    @Override
    public PyObject __truediv__(PyObject right) {
        return int___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___truediv___doc)
    final PyObject int___truediv__(PyObject right) {
        if (right instanceof PyInteger) {
            return __float__().__truediv__(right);
        } else if (right instanceof PyLong) {
            return int___long__().__truediv__(right);
        } else {
            return null;
        }
    }

    @Override
    public PyObject __rtruediv__(PyObject left) {
        return int___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rtruediv___doc)
    final PyObject int___rtruediv__(PyObject left) {
        if (left instanceof PyInteger) {
            return left.__float__().__truediv__(this);
        } else if (left instanceof PyLong) {
            return left.__truediv__(int___long__());
        } else {
            return null;
        }
    }

    private static long modulo(long x, long y, long xdivy) {
        return x - xdivy * y;
    }

    @Override
    public PyObject __mod__(PyObject right) {
        return int___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___mod___doc)
    final PyObject int___mod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerce(right);
        int v = getValue();
        return Py.newInteger(modulo(v, rightv, divide(v, rightv)));
    }

    @Override
    public PyObject __rmod__(PyObject left) {
        return int___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rmod___doc)
    final PyObject int___rmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        int leftv = coerce(left);
        int v = getValue();
        return Py.newInteger(modulo(leftv, v, divide(leftv, v)));
    }

    @Override
    public PyObject __divmod__(PyObject right) {
        return int___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___divmod___doc)
    final PyObject int___divmod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerce(right);

        int v = getValue();
        long xdivy = divide(v, rightv);
        return new PyTuple(Py.newInteger(xdivy), Py.newInteger(modulo(v, rightv, xdivy)));
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rdivmod___doc)
    final PyObject int___rdivmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        int leftv = coerce(left);

        int v = getValue();
        long xdivy = divide(leftv, v);
        return new PyTuple(Py.newInteger(xdivy), Py.newInteger(modulo(leftv, v, xdivy)));
    }

    @Override
    public PyObject __pow__(PyObject right, PyObject modulo) {
        return int___pow__(right, modulo);
    }

    @ExposedMethod(type = MethodType.BINARY, defaults = {"null"},
                   doc = BuiltinDocs.int___pow___doc)
    final PyObject int___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right)) {
            return null;
        }

        if (modulo != null && !canCoerce(modulo)) {
            return null;
        }

        return _pow(getValue(), coerce(right), modulo, this, right);
    }

    public PyObject __rpow__(PyObject left, PyObject modulo) {
        if (!canCoerce(left)) {
            return null;
        }

        if (modulo != null && !canCoerce(modulo)) {
            return null;
        }

        return _pow(coerce(left), getValue(), modulo, left, this);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rpow___doc)
    final PyObject int___rpow__(PyObject left) {
        return __rpow__(left, null);
    }

    private static PyObject _pow(int value, int pow, PyObject modulo, PyObject left,
                                 PyObject right) {
        int mod = 0;
        long tmp = value;
        boolean neg = false;
        if (tmp < 0) {
            tmp = -tmp;
            neg = (pow & 0x1) != 0;
        }
        long result = 1;

        if (pow < 0) {
            if (value != 0) {
                return left.__float__().__pow__(right, modulo);
            } else {
                throw Py.ZeroDivisionError("0.0 cannot be raised to a negative power");
            }
        }

        if (modulo != null) {
            mod = coerce(modulo);
            if (mod == 0) {
                throw Py.ValueError("pow(x, y, z) with z==0");
            }
        }

        // Standard O(ln(N)) exponentiation code
        while (pow > 0) {
            if ((pow & 0x1) != 0) {
                result *= tmp;
                if (mod != 0) {
                    result %= mod;
                }

                if (result > Integer.MAX_VALUE) {
                    return left.__long__().__pow__(right, modulo);
                }
            }
            pow >>= 1;
            if (pow == 0) {
                break;
            }
            tmp *= tmp;

            if (mod != 0) {
                tmp %= mod;
            }

            if (tmp > Integer.MAX_VALUE) {
                return left.__long__().__pow__(right, modulo);
            }
        }

        if (neg) {
            result = -result;
        }

        // Cleanup result of modulo
        if (mod != 0) {
            result = modulo(result, mod, divide(result, mod));
        }
        return Py.newInteger(result);
    }


    @Override
    public PyObject __lshift__(PyObject right) {
        return int___lshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___lshift___doc)
    final PyObject int___lshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger) {
            rightv = ((PyInteger) right).getValue();
        } else if (right instanceof PyLong) {
            return int___long__().__lshift__(right);
        } else {
            return null;
        }

        if (rightv >= Integer.SIZE) {
            return __long__().__lshift__(right);
        } else if (rightv < 0) {
            throw Py.ValueError("negative shift count");
        }
        int result = getValue() << rightv;
        if (getValue() != result >> rightv) {
            return __long__().__lshift__(right);
        }
        return Py.newInteger(result);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rlshift___doc)
    final PyObject int___rlshift__(PyObject left) {
        int leftv;
        if (left instanceof PyInteger) {
            leftv = ((PyInteger) left).getValue();
        } else if (left instanceof PyLong) {
            return left.__rlshift__(int___long__());
        } else {
            return null;
        }

        if (getValue() >= Integer.SIZE) {
            return left.__long__().__lshift__(this);
        } else if (getValue() < 0) {
            throw Py.ValueError("negative shift count");
        }
        int result = leftv << getValue();
        if (leftv != result >> getValue()) {
            return left.__long__().__lshift__(this);
        }
        return Py.newInteger(result);
    }

    @Override
    public PyObject __rshift__(PyObject right) {
        return int___rshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rshift___doc)
    final PyObject int___rshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger) {
            rightv = ((PyInteger) right).getValue();
        } else if (right instanceof PyLong) {
            return int___long__().__rshift__(right);
        } else {
            return null;
        }

        if (rightv < 0) {
            throw Py.ValueError("negative shift count");
        }

        if (rightv >= Integer.SIZE) {
            return Py.newInteger(getValue() < 0 ? -1 : 0);
        }

        return Py.newInteger(getValue() >> rightv);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rrshift___doc)
    final PyObject int___rrshift__(PyObject left) {
        int leftv;
        if (left instanceof PyInteger) {
            leftv = ((PyInteger) left).getValue();
        } else if (left instanceof PyLong) {
            return left.__rshift__(int___long__());
        } else {
            return null;
        }

        if (getValue() < 0) {
            throw Py.ValueError("negative shift count");
        }

        if (getValue() >= Integer.SIZE) {
            return Py.newInteger(leftv < 0 ? -1 : 0);
        }

        return Py.newInteger(leftv >> getValue());
    }

    @Override
    public PyObject __and__(PyObject right) {
        return int___and__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___and___doc)
    final PyObject int___and__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger) {
            rightv = ((PyInteger) right).getValue();
        } else if (right instanceof PyLong) {
            return int___long__().__and__(right);
        } else {
            return null;
        }

        return Py.newInteger(getValue() & rightv);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rand___doc)
    final PyObject int___rand__(PyObject left) {
        return int___and__(left);
    }

    @Override
    public PyObject __xor__(PyObject right) {
        return int___xor__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___xor___doc)
    final PyObject int___xor__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger) {
            rightv = ((PyInteger) right).getValue();
        } else if (right instanceof PyLong) {
            return int___long__().__xor__(right);
        } else {
            return null;
        }

        return Py.newInteger(getValue() ^ rightv);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___rxor___doc)
    final PyObject int___rxor__(PyObject left) {
        int leftv;
        if (left instanceof PyInteger) {
            leftv = ((PyInteger) left).getValue();
        } else if (left instanceof PyLong) {
            return left.__rxor__(int___long__());
        } else {
            return null;
        }

        return Py.newInteger(leftv ^ getValue());
    }

    @Override
    public PyObject __or__(PyObject right) {
        return int___or__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___or___doc)
    final PyObject int___or__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger) {
            rightv = ((PyInteger) right).getValue();
        } else if (right instanceof PyLong) {
            return int___long__().__or__(right);
        } else {
            return null;
        }

        return Py.newInteger(getValue() | rightv);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.int___ror___doc)
    final PyObject int___ror__(PyObject left) {
        return int___or__(left);
    }

    @Override
    public PyObject __neg__() {
        return int___neg__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___neg___doc)
    final PyObject int___neg__() {
        long x = getValue();
        long result = -x;
        // check for overflow
        if (x < 0 && result == x) {
            return new PyLong(x).__neg__();
        }
        return Py.newInteger(result);
    }

    @Override
    public PyObject __pos__() {
        return int___pos__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___pos___doc)
    final PyObject int___pos__() {
        return int___int__();
    }

    @Override
    public PyObject __abs__() {
        return int___abs__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___abs___doc)
    final PyObject int___abs__() {
        if (getValue() < 0) {
            return int___neg__();
        }
        return int___int__();
    }

    @Override
    public PyObject __invert__() {
        return int___invert__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___invert___doc)
    final PyObject int___invert__() {
        return Py.newInteger(~getValue());
    }

    @Override
    public PyObject __int__() {
        return int___int__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___int___doc)
    final PyInteger int___int__() {
        return getType() == TYPE ? this : Py.newInteger(getValue());
    }

    @Override
    public PyObject __long__() {
        return int___long__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___long___doc)
    final PyObject int___long__() {
        return new PyLong(getValue());
    }

    @Override
    public PyFloat __float__() {
        return int___float__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___float___doc)
    final PyFloat int___float__() {
        return new PyFloat((double) getValue());
    }

    @Override
    public PyObject __trunc__() {
        return int___trunc__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___trunc___doc)
    final PyObject int___trunc__() {
        return this;
    }

    @Override
    public PyObject conjugate() {
        return int_conjugate();
    }

    @ExposedMethod(doc = BuiltinDocs.int_conjugate_doc)
    final PyObject int_conjugate() {
        return this;
    }

    @Override
    public PyComplex __complex__() {
        return new PyComplex(getValue(), 0.);
    }

    @Override
    public PyString __oct__() {
        return int___oct__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___oct___doc)
    final PyString int___oct__() {
        if (getValue() < 0) {
            return new PyString("-0" + Integer.toString(getValue() * -1, 8));
        } else {
            return new PyString("0" + Integer.toString(getValue(), 8));
        }
    }

    @Override
    public PyString __hex__() {
        return int___hex__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___hex___doc)
    final PyString int___hex__() {
        if (getValue() < 0) {
            return new PyString("-0x" + Integer.toString(getValue() * -1, 16));
        } else {
            return new PyString("0x" + Integer.toString(getValue(), 16));
        }
    }

    @ExposedMethod(doc = BuiltinDocs.int___getnewargs___doc)
    final PyTuple int___getnewargs__() {
        return new PyTuple(new PyObject[]{new PyInteger(this.getValue())});
    }

    @Override
    public PyTuple __getnewargs__() {
        return int___getnewargs__();
    }

    @Override
    public PyObject __index__() {
        return int___index__();
    }

    @ExposedMethod(doc = BuiltinDocs.int___index___doc)
    final PyObject int___index__() {
        return this;
    }

    @Override
    public int bit_length() {
        return int_bit_length();
    }

    @ExposedMethod(doc = BuiltinDocs.int_bit_length_doc)
    final int int_bit_length() {
        int v = value;
        if (v < 0) {
            v = -v;
        }
        return BigInteger.valueOf(v).bitLength();
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return int___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.int___format___doc)
    final PyObject int___format__(PyObject formatSpec) {
        return formatImpl(getValue(), formatSpec);
    }

    static PyObject formatImpl(Object value, PyObject formatSpec) {
        if (!(formatSpec instanceof PyString)) {
            throw Py.TypeError("__format__ requires str or unicode");
        }

        PyString formatSpecStr = (PyString) formatSpec;
        String result;
        try {
            String specString = formatSpecStr.getString();
            InternalFormatSpec spec = new InternalFormatSpecParser(specString).parse();
            result = formatIntOrLong(value, spec);
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
        return formatSpecStr.createInstance(result);
    }

    /**
     * Formats an integer or long number according to a PEP-3101 format specification.
     *
     * @param value Integer or BigInteger object specifying the value to format.
     * @param spec parsed PEP-3101 format specification.
     * @return result of the formatting.
     */
    public static String formatIntOrLong(Object value, InternalFormatSpec spec) {
        if (spec.precision != -1) {
            throw new IllegalArgumentException("Precision not allowed in integer format specifier");
        }
        int sign;
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            sign = intValue < 0 ? -1 : intValue == 0 ? 0 : 1;
        } else {
            sign = ((BigInteger) value).signum();
        }
        String strValue;
        if (spec.type == 'c') {
            if (spec.sign != '\0') {
                throw new IllegalArgumentException("Sign not allowed with integer format "
                                                   + "specifier 'c'");
            }
            if (value instanceof Integer) {
                int intValue = (Integer) value;
                if (intValue > 0xffff) {
                    throw new IllegalArgumentException("%c arg not in range(0x10000)");
                }
                strValue = Character.toString((char) intValue);
            } else {
                BigInteger bigInt = (BigInteger) value;
                if (bigInt.intValue() > 0xffff || bigInt.bitCount() > 16) {
                    throw new IllegalArgumentException("%c arg not in range(0x10000)");
                }
                strValue = Character.toString((char) bigInt.intValue());
            }
        } else {
            int radix = 10;
            if (spec.type == 'o') {
                radix = 8;
            } else if (spec.type == 'x' || spec.type == 'X') {
                radix = 16;
            } else if (spec.type == 'b') {
                radix = 2;
            }

            if (spec.type == 'n') {
                strValue = NumberFormat.getNumberInstance().format(value);
            } else if (spec.thousands_separators) {
                NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
                format.setGroupingUsed(true);
                strValue = format.format(value);
            } else if (value instanceof BigInteger) {
                strValue = ((BigInteger) value).toString(radix);
            } else {
                strValue = Integer.toString((Integer) value, radix);
            }

            if (spec.alternate) {
                String strPrefix = "";
                switch (radix) {
                    case 2:
                        strPrefix = "0b";
                        break;
                    case 8:
                        strPrefix = "0o";
                        break;
                    case 16:
                        strPrefix = "0x";
                        break;
                }

                if (strValue.startsWith("-")) {
                    //assert (sign < 0);
                    if (!strPrefix.equals(""))
                        strValue = "-" + strPrefix + strValue.substring(1, strValue.length());
                } else {
                    strValue = strPrefix + strValue;
                }
            }

            if (spec.type == 'X') {
                strValue = strValue.toUpperCase();
            }

            if (sign >= 0) {
                if (spec.sign == '+') {
                    strValue = "+" + strValue;
                } else if (spec.sign == ' ') {
                    strValue = " " + strValue;
                }
            }
        }
        if (spec.align == '=' && (sign < 0 || spec.sign == '+' || spec.sign == ' ')) {
            char signChar = strValue.charAt(0);
            return signChar + spec.pad(strValue.substring(1), '>', 1);
        }
        return spec.pad(strValue, '>', 0);
    }

    @Override
    public boolean isIndex() {
        return true;
    }

    @Override
    public int asIndex(PyObject err) {
        return getValue();
    }

    @Override
    public boolean isMappingType() {
        return false;
    }

    @Override
    public boolean isNumberType() {
        return true;
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }

    @Override
    public long asLong(int index) {
        return getValue();
    }

    @Override
    public int asInt(int index) {
        return getValue();
    }

    @Override
    public int asInt() {
        return getValue();
    }

    @Override
    public long asLong() {
        return getValue();
    }
}
