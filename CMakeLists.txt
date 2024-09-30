# CMakeList.txt : CMake project for ZDesktopManager, include source and define
# project specific logic here.
#
cmake_minimum_required (VERSION 3.8)

# Enable Hot Reload for MSVC compilers if supported.
if (POLICY CMP0141)
  cmake_policy(SET CMP0141 NEW)
  set(CMAKE_MSVC_DEBUG_INFORMATION_FORMAT "$<IF:$<AND:$<C_COMPILER_ID:MSVC>,$<CXX_COMPILER_ID:MSVC>>,$<$<CONFIG:Debug,RelWithDebInfo>:EditAndContinue>,$<$<CONFIG:Debug,RelWithDebInfo>:ProgramDatabase>>")
endif()

project ("ZDesktopManager")


find_package(wxWidgets REQUIRED)
include(${wxWidgets_USE_FILE})
include_directories(
        ${PROJECT_SOURCE_DIR}/include
        ${PROJECT_SOURCE_DIR}/src
)
file(GLOB all_SRCS
        "${PROJECT_SOURCE_DIR}/include/*.h"
        "${PROJECT_SOURCE_DIR}/include/*.hpp"
        "${PROJECT_SOURCE_DIR}/src/*.cpp"
        "${PROJECT_SOURCE_DIR}/src/*.c"
        )


# Add source to this project's executable.
add_executable (ZDesktopManager WIN32 ${all_SRCS})

set_target_properties(
    ZDesktopManager
    PROPERTIES LINK_FLAGS_DEBUG "/SUBSYSTEM:CONSOLE"
               LINK_FLAGS_RELEASE "/SUBSYSTEM:WINDOWS /ENTRY:mainCRTStartup"
)

target_link_libraries(ZDesktopManager ${wxWidgets_LIBRARIES} advapi32)

# DPI感知
target_sources(ZDesktopManager PRIVATE wx_dpi_aware.manifest)

if (CMAKE_VERSION VERSION_GREATER 3.12)
  set_property(TARGET ZDesktopManager PROPERTY CXX_STANDARD 20)
endif()

# TODO: Add tests and install targets if needed.
