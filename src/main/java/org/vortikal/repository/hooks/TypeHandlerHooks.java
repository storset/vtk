/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.repository.hooks;

import java.io.InputStream;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.InheritablePropertiesStoreContext;
import org.vortikal.repository.NoSuchContentException;
import org.vortikal.repository.RepositoryImpl;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.content.ContentRepresentationRegistry;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.store.ContentStore;

/**
 * Expert API for customized resource type handling. Allows hooking into basic
 * repository resource opertions. To be used where specialized code/behaviour is
 * required for processing resource content.
 *
 * <p>
 * This extension mechanism is focused on resources and content. Security is
 * always handled by the repository itself, as well as aspects not directly
 * related to resource type, properties or content. It does however give you
 * every opportunity to shoot yourself in the foot if doing anything bad in the
 * hook methods. So if you're going to implement hooks, you should be very
 * familiar with the {@link RepositoryImpl core repository code} itself.
 *
 * <p>
 * All methods in this interface will be called within the transcational context
 * of the corresponding operation. If an exception is thrown from any of these
 * hook methods, the transaction will be aborted and the repository operation
 * will fail as a whole.
 * 
 * <p>Repository does not allow multiple hook implementations for the same
 * content or resource types. If such a configuration is detected, the repository
 * will throw errors at initialization time.
 *
 * <p>
 * Certain aspects of the repository are currently not supported by hooks in
 * this interface:
 * <ul>
 * <li>Revisions
 * <li>Comments
 * <li>Restorable resources (trash can)
 * <li>Locking
 * <li>ACL and security-related operations.
 * </ul>
 */
public interface TypeHandlerHooks {

    /**
     * Will be called by repository to determine the content type (media type)
     * for which this instance applies. Used when new resources are created.
     *
     * @return A string identifying the content type. May be only the overall
     * media type group (string before "/", like "video/") or a specific fully
     * qualified type/subtype.
     */
    String getApplicableContent();

    /**
     * Will be called by repository to determine what resource type the hooks
     * apply for.
     *
     * <p>
     * Certain types are reserved and cannot have associated hooks. In case such
     * a type is return by this method, an error will be thrown by repository at
     * initialization time.
     *
     * <p>
     * Reserved types are:
     * <ul><li>resource
     * <li>collection
     * <li>file
     * </ul>
     *
     * @return string with the type identifier.
     */
    String getApplicableResourceType();

    /**
     * Called once by repository at initialization time to provide access to the
     * default content store. If you need this in your hooks, you should store a
     * reference to it in this method.
     *
     * @param repositoryContentStore the repository default content store.
     */
    void setContentStore(ContentStore repositoryContentStore);

    /**
     * Called once by repository at initialization time to provide access to the
     * {@link ContentRepresentationRegistry}. If you need this in your hooks,
     * you should store a reference to it.
     *
     * @param contentRepresentationRegistry an instance of
     * {@link ContentRepresentationRegistry} used by repository.
     */
    void setContentRepresentationRegistry(ContentRepresentationRegistry contentRepresentationRegistry);

    /**
     * Called by repository to get an instance of {@link Content} for evaluation
     * purposes.
     *
     * @param resource the resource to be evaluated
     * @param defaultContent the default {@link Content} implementation for the
     * resource
     * @return an instance of {@link Content} which will be used instead of the
     * default.
     * @throws Exception in case of errors.
     */
    Content getContentForEvaluation(ResourceImpl resource, Content defaultContent) throws Exception;

    /**
     * Called by repository for getting an alternative resource content stream
     * using a content identifier.
     *
     * @param resource the resource
     * @param contentIdentifier an implementation specific content identifier
     * @return an instance of {@link ContentStream} with alternative content
     * @throws NoSuchContentException if no such content is available
     * @throws Exception in case of errors
     */
    ContentStream onGetAlternativeContentStream(ResourceImpl resource, String contentIdentifier) throws NoSuchContentException, Exception;

    /**
     * Called by repository just before returning the resource in
     * {@link Repository#retrieve(java.lang.String, org.vortikal.repository.Path, boolean) retrieve}.
     *
     * @param resource The resource to be returned by repository. May be freely
     * modified.
     * @return the resource (perhaps modified).
     * @throws Exception in case of errors.
     */
    ResourceImpl onRetrieve(ResourceImpl resource) throws Exception;

    /**
     * Called just before returning list of children in
     * {@link Repository#listChildren(java.lang.String, org.vortikal.repository.Path, boolean) listChildren}.
     *
     * @param children list of resources to be returned. May be modified freely.
     * @return a list of children to be returned to client code.
     * @throws Exception in case of errors.
     */
    ResourceImpl[] onListChildren(ResourceImpl[] children) throws Exception;

    /**
     * Called before storing resource in
     * {@link Repository#store(java.lang.String, org.vortikal.repository.Resource) store}.
     * The method is called <em>before</em> property evaluation and actual store
     * occurs.
     *
     * @param resource The resource to be evaluated and stored (may be
     * modified).
     * @return The resource to be evaluated and stored.
     * @throws Exception in case of errors.
     */
    ResourceImpl onStore(ResourceImpl resource) throws Exception;

    /**
     * Called before storing resource in
     * {@link Repository#store(java.lang.String, org.vortikal.repository.Resource, org.vortikal.repository.StoreContext) store}
     * when a {@link SystemChangeContext system change store context} is
     * provided.
     *
     * The method is called <em>before</em> property evaluation and actual store
     * occurs.
     *
     * @param resource The resource to be evaluated and stored (may be
     * modified).
     * @param ctx a {@link SystemChangeContext}
     * @return The resource to be evaluated and stored.
     * @throws Exception in case of errors.
     */
    ResourceImpl onStoreSystemChange(ResourceImpl resource, SystemChangeContext ctx) throws Exception;

    /**
     * Called before storing resource in
     * {@link Repository#store(java.lang.String, org.vortikal.repository.Resource, org.vortikal.repository.StoreContext) store}
     * when a {@link SystemChangeContext system change store context} is
     * provided.
     *
     * The method is called <em>before</em> property evaluation and actual store
     * occurs.
     *
     * @param resource The resource to be evaluated and stored (may be
     * modified).
     * @param ctx an {@link InheritablePropertiesStoreContext}
     * @return The resource to be evaluated and stored.
     * @throws Exception in case of errors.
     */
    ResourceImpl onStoreInheritableProps(ResourceImpl resource, InheritablePropertiesStoreContext ctx) throws Exception;

    /**
     * Called before {@link Repository#storeContent(java.lang.String, org.vortikal.repository.Path, java.io.InputStream)
     * storing content} for a resource.
     *
     * <p>
     * This method is called <em>before</em> property evaluation takes place.
     *
     * <p>
     * <strong>The implementation is given the responsibility of actually
     * storing the provided content stream. The repository will not store to the
     * default content store ! This is in contrast to other hooks available,
     * since the input stream can generally only be consumed once.
     * </strong>
     *
     * @param resource the resource for which content is being stored.
     * @param stream the input stream with data to store.
     * @param contentType the guessed content type of the input stream.
     * @return the resource (may be modified)
     * @throws Exception in case of errors
     */
    ResourceImpl onStoreContent(ResourceImpl resource, InputStream stream, String contentType) throws Exception;

    /**
     * Hook method called when {@link Repository#getInputStream(java.lang.String, org.vortikal.repository.Path, boolean)
     * input stream} is requested for a resource.
     *
     * <p>
     * <strong>This method is given the responsibility of providing the input
     * stream. The repository will not provide the default input stream from the
     * content store when this method is used.</strong>
     *
     * @param resource the resource to fetch content from
     * @return an input stream for the resource
     * @throws Exception in case of errors
     */
    InputStream onGetInputStream(ResourceImpl resource) throws Exception;

    /**
     * Hook method called on {@link Repository#createDocument(java.lang.String, org.vortikal.repository.Path, java.io.InputStream)
     * document creation}.
     *
     * The method will be called <em>before</em> property evaluation takes
     * place.
     *
     * <p>
     * <strong>When this method is used, it is given the responsibility of
     * actually storing the content somewhere. (Repository will not store to its
     * default content store, but hook impl is free to do so itself.)</strong>
     *
     * @param resource the new resource being created
     * @param stream the input stream with the content
     * @param contentType the guessed content type of the input stream.
     * @return the resource to be stored (may be modified).
     *
     * @throws Exception in case of errors
     */
    ResourceImpl onCreateDocument(ResourceImpl resource,
            InputStream stream, String contentType) throws Exception;

    /**
     * Hook method called when copying a resource takes place.
     *
     * <p>
     * Hook will be called <em>before name change evaluation takes place</em>
     * on the destination resource.
     *
     * @param src The source resource.
     * @param dst The destination resource before property evaluation.
     *
     * @throws Exception in case of errors.
     */
    void onCopy(ResourceImpl src, ResourceImpl dst) throws Exception;

    /**
     * Hook method called when moving/renaming a resource.
     *
     * <p>
     * Hook will be called <em>before name change evaluation takes place</em>
     * on the destination resource.
     *
     * @param src The source resource.
     * @param dst The destination resource before property evaluation.
     *
     * @throws Exception in case of errors.
     */
    void onMove(ResourceImpl src, ResourceImpl dst) throws Exception;

    /**
     * Hook method called when deleting a resource.
     *
     * <p>
     * Hook will be called <em>before</em> resource is actually deleted from the
     * database and content store. The resource may possibly be moved to trash
     * store in this operation.
     *
     * @param resource the resource which will be deleted
     * @param restorable whether the delete is to trash or not
     * @throws Exception in case of errors
     */
    void onDelete(ResourceImpl resource, boolean restorable) throws Exception;

}
