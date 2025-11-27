package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal;
import org.gradle.api.internal.artifacts.DefaultProjectDependencyFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.catalog.DelegatingProjectDependency;
import org.gradle.api.internal.catalog.TypeSafeProjectDependencyFactory;
import javax.inject.Inject;

@NonNullApi
public class FeatureProjectDependency extends DelegatingProjectDependency {

    @Inject
    public FeatureProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":feature:chat"
     */
    public Feature_ChatProjectDependency getChat() { return new Feature_ChatProjectDependency(getFactory(), create(":feature:chat")); }

    /**
     * Creates a project dependency on the project at path ":feature:connectivity"
     */
    public Feature_ConnectivityProjectDependency getConnectivity() { return new Feature_ConnectivityProjectDependency(getFactory(), create(":feature:connectivity")); }

    /**
     * Creates a project dependency on the project at path ":feature:media"
     */
    public Feature_MediaProjectDependency getMedia() { return new Feature_MediaProjectDependency(getFactory(), create(":feature:media")); }

    /**
     * Creates a project dependency on the project at path ":feature:usercenter"
     */
    public Feature_UsercenterProjectDependency getUsercenter() { return new Feature_UsercenterProjectDependency(getFactory(), create(":feature:usercenter")); }

}
