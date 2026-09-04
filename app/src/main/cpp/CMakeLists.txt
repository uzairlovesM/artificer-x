cmake_minimum_required(VERSION 3.22.1)
project("artificerx_native" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

option(ARTIFICERX_NATIVE_ENABLE_SIMD "Enable compiler optimizations for native raster kernels" ON)

if(ARTIFICERX_NATIVE_ENABLE_SIMD AND ANDROID)
    add_compile_options(-O3 -ffast-math -fomit-frame-pointer)
endif()

add_library(artificerx_native SHARED artificerx_native.cpp raster_quality.cpp)

target_compile_definitions(artificerx_native PRIVATE ARTIFICERX_NATIVE_ABI=1)
target_link_libraries(artificerx_native android log)
