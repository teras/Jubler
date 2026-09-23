cmake_minimum_required(VERSION 3.19)
project(jubler-qt VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_AUTOMOC ON)
set(CMAKE_AUTORCC ON)
set(CMAKE_AUTOUIC ON)

find_package(Qt6 REQUIRED COMPONENTS Core Gui Widgets OpenGLWidgets Network Svg Concurrent Xml LinguistTools)

# libmpv: prefer pkg-config, fall back to plain find_library.
find_package(PkgConfig)
if(PkgConfig_FOUND)
    pkg_check_modules(MPV IMPORTED_TARGET mpv)
endif()
if(NOT MPV_FOUND)
    find_library(MPV_LIBRARY mpv REQUIRED)
    find_path(MPV_INCLUDE_DIR mpv/client.h REQUIRED)
endif()
# hunspell (spell checking) and libcrypto (AES-GCM for stored secrets).
pkg_check_modules(HUNSPELL REQUIRED IMPORTED_TARGET hunspell)
pkg_check_modules(CRYPTO REQUIRED IMPORTED_TARGET libcrypto)
pkg_check_modules(ZLIB REQUIRED IMPORTED_TARGET zlib)

# --- Toolkit-agnostic core --------------------------------------------------
# Model, time, styles, formats, tool algorithms, options, OS paths. Depends on
# Qt Core/Gui only (no Widgets) so it is unit-testable headlessly.
file(GLOB_RECURSE JUBLER_CORE_SOURCES CONFIGURE_DEPENDS src/core/*.cpp)
add_library(jubler-core STATIC ${JUBLER_CORE_SOURCES})
target_include_directories(jubler-core PUBLIC src src/plugin-api)
target_link_libraries(jubler-core PUBLIC Qt6::Core Qt6::Gui Qt6::Network Qt6::Xml PkgConfig::HUNSPELL PkgConfig::CRYPTO PkgConfig::ZLIB)

# --- Application ------------------------------------------------------------
option(JUBLER_BUILD_APP "Build the GUI application" ON)
if(JUBLER_BUILD_APP)
file(GLOB_RECURSE JUBLER_APP_SOURCES CONFIGURE_DEPENDS src/app/*.cpp)
add_executable(jubler-qt ${JUBLER_APP_SOURCES} resources/jubler.qrc)
# The version is the latest "v<version>" tag (as the Gradle build did).
set(JUBLER_VERSION_STRING "0.0.0-SNAPSHOT")
find_package(Git QUIET)
if(GIT_FOUND)
    execute_process(COMMAND "${GIT_EXECUTABLE}" describe --tags --abbrev=0 --match "v[0-9]*"
                    WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
                    OUTPUT_VARIABLE _jubler_tag OUTPUT_STRIP_TRAILING_WHITESPACE
                    RESULT_VARIABLE _jubler_tag_rc ERROR_QUIET TIMEOUT 5)
    if(_jubler_tag_rc EQUAL 0 AND _jubler_tag MATCHES "^[vV](.+)$")
        set(JUBLER_VERSION_STRING "${CMAKE_MATCH_1}")
    endif()
endif()
message(STATUS "Jubler version: ${JUBLER_VERSION_STRING}")
set(JUBLER_RELEASE -1 CACHE STRING "Release counter used by the update check (-1 = development build)")
option(JUBLER_PACKAGED "Distribution-packaged build (no self-update)" OFF)
target_compile_definitions(jubler-qt PRIVATE
    JUBLER_VERSION="${JUBLER_VERSION_STRING}"
    JUBLER_RELEASE=${JUBLER_RELEASE}
    JUBLER_PACKAGED=$<BOOL:${JUBLER_PACKAGED}>)
# The installed program is "jubler", as the Java build's launcher.
set_target_properties(jubler-qt PROPERTIES OUTPUT_NAME jubler)
target_link_libraries(jubler-qt PRIVATE
    jubler-core
    Qt6::Widgets
    Qt6::OpenGLWidgets
    Qt6::Svg
    Qt6::Concurrent
)
# Translations: resources/i18n/*.ts (generated from resources/i18n-src by
# tools/i18n-json2ts.py) compiled and embedded under :/i18n.
file(GLOB JUBLER_TS_FILES CONFIGURE_DEPENDS resources/i18n/*.ts)
qt_add_lrelease(jubler-qt TS_FILES ${JUBLER_TS_FILES} QM_FILES_OUTPUT_VARIABLE JUBLER_QM_FILES)
qt_add_resources(jubler-qt "translations" PREFIX "/i18n" BASE "${CMAKE_CURRENT_BINARY_DIR}" FILES ${JUBLER_QM_FILES})
if(MPV_FOUND)
    target_link_libraries(jubler-qt PRIVATE PkgConfig::MPV)
else()
    target_include_directories(jubler-qt PRIVATE ${MPV_INCLUDE_DIR})
    target_link_libraries(jubler-qt PRIVATE ${MPV_LIBRARY})
endif()
# The FFmpeg libraries libmpv itself links, used directly for probing, the
# waveform and the recipe WAV (never a second FFmpeg copy).
pkg_check_modules(LIBAV REQUIRED IMPORTED_TARGET libavformat libavcodec libavutil libswresample)
target_link_libraries(jubler-qt PRIVATE PkgConfig::LIBAV)
# Guard: the FFmpeg headers must be the major versions libmpv loads.
if(UNIX AND NOT APPLE)
    if(MPV_FOUND)
        find_library(_jubler_mpv_so mpv HINTS ${MPV_LIBRARY_DIRS})
    else()
        set(_jubler_mpv_so ${MPV_LIBRARY})
    endif()
    find_program(READELF_EXECUTABLE readelf)
    if(READELF_EXECUTABLE AND _jubler_mpv_so)
        execute_process(COMMAND ${READELF_EXECUTABLE} -d ${_jubler_mpv_so} OUTPUT_VARIABLE _jubler_mpv_needed ERROR_QUIET)
        foreach(_lib avformat avcodec avutil swresample)
            string(REGEX MATCH "^[0-9]+" _major "${LIBAV_lib${_lib}_VERSION}")
            string(REGEX MATCH "lib${_lib}\\.so\\.([0-9]+)" _found "${_jubler_mpv_needed}")
            if(_found AND NOT CMAKE_MATCH_1 STREQUAL _major)
                message(FATAL_ERROR "libmpv links lib${_lib}.so.${CMAKE_MATCH_1} but the ${_lib} headers are version ${_major}: "
                                    "building would load a second FFmpeg copy. Install the FFmpeg development files matching libmpv.")
            endif()
        endforeach()
    endif()
endif()
endif()

# --- Installation (Linux/BSD; the macOS bundle and the Windows installer are
# built by the packaging scripts) --------------------------------------------
if(JUBLER_BUILD_APP AND UNIX AND NOT APPLE)
    include(GNUInstallDirs)
    install(TARGETS jubler-qt RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR})
    install(FILES packaging/linux/com.panayotis.jubler.desktop DESTINATION ${CMAKE_INSTALL_DATADIR}/applications)
    install(FILES packaging/linux/com.panayotis.jubler.metainfo.xml DESTINATION ${CMAKE_INSTALL_DATADIR}/metainfo)
    install(FILES packaging/linux/com.panayotis.jubler.mime.xml DESTINATION ${CMAKE_INSTALL_DATADIR}/mime/packages
            RENAME com.panayotis.jubler.xml)
    install(FILES resources/logo/logo.svg DESTINATION ${CMAKE_INSTALL_DATADIR}/icons/hicolor/scalable/apps
            RENAME com.panayotis.jubler.svg)
    install(FILES LICENSE DESTINATION ${CMAKE_INSTALL_DATADIR}/licenses/jubler OPTIONAL)
endif()

# --- Tests ------------------------------------------------------------------
enable_testing()
file(GLOB JUBLER_TEST_SOURCES CONFIGURE_DEPENDS tests/test_*.cpp)
# The example plugin, built for test_plugins (which loads it).
add_library(exampleplugin MODULE examples/plugin/ExamplePlugin.cpp)
target_include_directories(exampleplugin PRIVATE src/plugin-api)
target_link_libraries(exampleplugin PRIVATE Qt6::Core Qt6::Gui Qt6::Widgets)
set_target_properties(exampleplugin PROPERTIES PREFIX "" AUTOMOC ON)
foreach(test_src ${JUBLER_TEST_SOURCES})
    get_filename_component(test_name ${test_src} NAME_WE)
    add_executable(${test_name} ${test_src} tests/TestSupport.h)
    target_link_libraries(${test_name} PRIVATE jubler-core PkgConfig::ZLIB)
    target_include_directories(${test_name} PRIVATE tests)
    add_test(NAME ${test_name} COMMAND ${test_name} ${CMAKE_CURRENT_SOURCE_DIR}/tests/fixtures)
endforeach()
add_dependencies(test_plugins exampleplugin)
target_compile_definitions(test_plugins PRIVATE JUBLER_EXAMPLE_PLUGIN="$<TARGET_FILE:exampleplugin>")
