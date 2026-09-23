# An out-of-tree Jubler plugin using every extension point. Build with
#   cmake -S examples/plugin -B build-plugin -DJUBLER_API_DIR=<path to src/plugin-api>
# and drop the resulting library into the per-user plugins directory.
cmake_minimum_required(VERSION 3.19)
project(jubler-example-plugin LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_AUTOMOC ON)
find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets)
set(JUBLER_API_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../src/plugin-api" CACHE PATH "Jubler plugin API headers")
add_library(exampleplugin MODULE ExamplePlugin.cpp)
target_include_directories(exampleplugin PRIVATE ${JUBLER_API_DIR})
target_link_libraries(exampleplugin PRIVATE Qt6::Core Qt6::Gui Qt6::Widgets)
set_target_properties(exampleplugin PROPERTIES PREFIX "")
