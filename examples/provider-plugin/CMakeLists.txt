# A minimal out-of-tree subtitle-download provider plugin. Build with
#   cmake -S examples/provider-plugin -B build-plugin -DJUBLER_API_DIR=<path to src/plugin-api>
# and drop the resulting library into the per-user plugins directory.
cmake_minimum_required(VERSION 3.19)
project(jubler-example-provider LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_AUTOMOC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Widgets)
set(JUBLER_API_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../src/plugin-api" CACHE PATH "Jubler plugin API headers")
add_library(exampleprovider MODULE ExampleProvider.cpp)
target_include_directories(exampleprovider PRIVATE ${JUBLER_API_DIR})
target_link_libraries(exampleprovider PRIVATE Qt6::Core Qt6::Widgets)
set_target_properties(exampleprovider PROPERTIES PREFIX "")
