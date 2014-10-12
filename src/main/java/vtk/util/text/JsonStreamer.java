/* Copyright (c) 2014 University of Oslo, Norway
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package vtk.util.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Generate JSON directly to a stream using a stream friendly API. The stream
 * generator is stateful with regard to validation and should prevent client
 * code from creating invalid JSON. It handles comma-separators, value
 * serializing and proper closing of JSON structures.
 *
 * <p>
 * Client code invokes methods to add data forming a logical JSON structure. The
 * serialized data is immediately written to a stream writer instance, but state
 * is kept wrt. to JSON syntax. If client invokes generator methods in wrong
 * order, etc, typically <code>IllegalStateException</code> will be thrown to
 * prevent writing invalid JSON. This condition generally indicates an error in
 * the calling code.
 *
 * <p>
 * It uses {@link org.json.simple.JSONObject} and
 * {@link org.json.simple.JSONValue} internally for printing JSON values and
 * escaping strings.
 *
 * @author oyvind
 */
public class JsonStreamer {
    
    private enum State {
        INITIAL, ARRAY, OBJECT, KEY_VALUE
    }
    
    private static final class Context {
        State state;
        boolean empty;
        Context(JsonStreamer.State state) {
            this.state = state;
            this.empty = true;
        }
    }
    
    private final Deque<Context> contexts = new LinkedList<>();
    private final Writer w;

    /**
     * Construct instance writing JSON data to the provided writer.
     * @param w writer stream to write data to
     */
    public JsonStreamer(Writer w) {
        this.w = w;
        contexts.offerFirst(new Context(State.INITIAL));
    }

    /**
     * Start a new array.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is object with no specified key.
     */
    public JsonStreamer beginArray() throws IOException {
        Context ctx = contexts.peek();
        if (ctx.state == State.OBJECT) {
            throw new IllegalStateException("Cannot begin array in current stream state " + ctx.state);
        }
        if (!ctx.empty && ctx.state != State.KEY_VALUE) {
            w.write(',');
        }
        
        contexts.offerFirst(new Context(State.ARRAY));
        w.write('[');
        return this;
    }

    /**
     * End current array, forming a complete array value.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is not an array.
     */
    public JsonStreamer endArray() throws IOException {
        Context ctx = contexts.poll();
        if (ctx.state != State.ARRAY) {
            throw new IllegalStateException("Cannot end array in current stream state " + ctx.state);
        }
        
        w.write(']');
        
        Context enclosing = contexts.peek();
        enclosing.empty = false;
        if (enclosing.state == State.KEY_VALUE) {
            enclosing.state = State.OBJECT;
        }
        return this;
    }

    /**
     * Begin a new object.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if not outermost object and current state
     * is object with no key specified.
     */
    public JsonStreamer beginObject() throws IOException {
        Context ctx = contexts.peek();
        if (ctx.state == State.OBJECT){
            throw new IllegalStateException("Cannot begin object in current stream state " + ctx.state);
        }
        
        if (!ctx.empty && ctx.state != State.KEY_VALUE) {
            w.write(',');
        }
        
        contexts.offerFirst(new Context(State.OBJECT));
        w.write('{');
        return this;
    }

    /**
     * End current object, forming a complete object value.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is not an object.
     */
    public JsonStreamer endObject() throws IOException {
        Context objCtx = contexts.poll();
        if (objCtx.state != State.OBJECT) {
            throw new IllegalStateException("Cannot end object in current stream state " + objCtx.state);
        }
        
        w.write('}');
        
        Context up = contexts.peek();
        up.empty = false;
        if (up.state == State.KEY_VALUE) {
            up.state = State.OBJECT;
        }
        return this;
    }

    /**
     * Begin a member by specifying the key.
     * @param key key as string
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is not an object.
     */
    public JsonStreamer key(String key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        Context ctx = contexts.peek();
        if (ctx.state != State.OBJECT) {
            throw new IllegalStateException("Cannot start key in current stream state " + ctx.state);
        }
        
        if (!ctx.empty) {
            w.write(',');
        }
        JSONValue.writeJSONString(key, w);
        w.write(':');
        ctx.state = State.KEY_VALUE;
        return this;
    }

    /**
     * Write a value, which can be anything from primitives to full maps forming
     * complete objects.
     * @param object the value
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is object with no specified key.
     */
    public JsonStreamer value(Object object) throws IOException {
        Context ctx = contexts.peek();
        if (ctx.state == State.OBJECT) {
            throw new IllegalStateException("Cannot insert value in current stream state " + ctx.state);
        }

        if (!ctx.empty && ctx.state != State.KEY_VALUE) {
            w.write(',');
        }
        
        JSONValue.writeJSONString(object, w);

        if (ctx.state == State.KEY_VALUE) {
            ctx.state = State.OBJECT;
        }
        ctx.empty = false;
        return this;
    }

    /**
     * Write a complete object.
     * 
     * @param map the object
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is object with no specified key.
     */
    public JsonStreamer object(Map<String, Object> map) throws IOException {
        Context ctx = contexts.peek();
        if (ctx.state == State.OBJECT) {
            throw new IllegalStateException("Cannot insert object in current stream state " + ctx.state);
        }

        if (!ctx.empty && ctx.state != State.KEY_VALUE) {
            w.write(',');
        }
        
        JSONObject.writeJSONString(map, w);
        if (ctx.state == State.KEY_VALUE) {
            ctx.state = State.OBJECT;
        }
        ctx.empty = false;
        return this;
    }

    /**
     * Write an object member consisting of a key and a value.
     * @param key the key, cannot be <code>null</code>
     * @param value the value, which can be of any supported type.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is not an object
     */
    public JsonStreamer member(String key, Object value) throws IOException {
        Context ctx = contexts.peek();
        if (ctx.state != State.OBJECT) {
            throw new IllegalStateException("Cannot insert member in non-object stream state " + ctx.state);
        }

        if (!ctx.empty) {
            w.write(',');
        }
        
        JSONValue.writeJSONString(key, w);
        w.write(':');
        JSONValue.writeJSONString(value, w);

        ctx.empty = false;
        return this;
    }
    
    /**
     * Like {@link #member(java.lang.String, java.lang.Object) }, but only write
     * the member if both key and value are non-null.
     * @param key the key, may be <code>null</code>.
     * @param value the value, which can be of any supported kind.
     * @return this <code>JsonStreamer</code> instance.
     * @throws IOException if writing to stream fails
     * @throws IllegalStateException if current state is not an object
     */
    public JsonStreamer memberIfNotNull(String key, Object value) throws IOException {
        if (key == null || value == null) {
            return this;
        }
        
        return member(key, value);
    }

    /**
     * Gracefully ends current JSON data by syntactically closing all unclosed scopes
     * and verifying that object or array can be ended in current state without violating
     * JSON syntax.
     * 
     * <p>Note that this method does NOT close the underlying {@link Writer} instance.
     * 
     * @throws IOException 
     */
    public void endJson() throws IOException {
        Context ctx;
        Context previous = null;
        while ((ctx = contexts.poll()) != null) {
            switch (ctx.state) {
                case ARRAY:
                    w.write(']');
                    break;
                case OBJECT:
                    w.write('}');
                    break;
                case KEY_VALUE:
                    if (previous == null) {
                        throw new IllegalStateException("Cannot end stream in state " + ctx.state);
                    } else {
                        w.write('}');
                    }
                    break;
                case INITIAL:
                    contexts.offerFirst(ctx);
                    return;
            }
            previous = ctx;
        }
    }
}
