if(NOT TARGET oboe::oboe)
add_library(oboe::oboe SHARED IMPORTED)
set_target_properties(oboe::oboe PROPERTIES
    IMPORTED_LOCATION "C:/Users/kic/.gradle/caches/transforms-3/76d7a26834e433e53154d34200c47d62/transformed/oboe-1.7.0/prefab/modules/oboe/libs/android.arm64-v8a/liboboe.so"
    INTERFACE_INCLUDE_DIRECTORIES "C:/Users/kic/.gradle/caches/transforms-3/76d7a26834e433e53154d34200c47d62/transformed/oboe-1.7.0/prefab/modules/oboe/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

