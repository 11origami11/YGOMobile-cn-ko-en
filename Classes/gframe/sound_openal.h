#ifndef YGOPEN_SOUND_OPENAL_H
#define YGOPEN_SOUND_OPENAL_H

#include <memory>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <AL/al.h>
#include <AL/alc.h>

namespace YGOpen {

/* Modified from minetest: src/client/sound.h, src/client/sound_openal.cpp
 * https://github.com/minetest/minetest
 * Licensed under GNU LGPLv2.1
 */

struct OpenALSoundBuffer
{
    ALenum format;
    ALsizei frequency;
    ALuint id;
    std::vector<char> buffer;
};

class OpenALSingleton {
public:
    OpenALSingleton();
    ~OpenALSingleton();
    std::unique_ptr<ALCdevice, void (*)(ALCdevice* ptr)> device;
    std::unique_ptr<ALCcontext, void(*)(ALCcontext* ptr)> context;
};

class OpenALSoundLayer {
public:
    OpenALSoundLayer(const std::unique_ptr<OpenALSingleton>& openal);
    ~OpenALSoundLayer();

    bool load(const std::string& filename);

    int play2D(const std::string& filename, bool loop = false, bool music = false);
    int playMusic(const std::string& filename, bool loop = false, bool music = true) {
        return play2D(filename, loop, music);
    }

    bool exists(int sound);

    void stop(int sound);

    void stopAll();
    void stopAllSounds() {
        return stopAll();
    }

    void setSoundVolume(float gain);

    bool isCurrentlyPlaying(const std::string& name);

	std::string GetFileName(std::string file) {
		std::replace(file.begin(), file.end(), '\\', '/');
		size_t dashpos = file.find_last_of("/");
		if(dashpos == std::wstring::npos)
			dashpos = 0;
		else
			dashpos++;
		size_t dotpos = file.find_last_of(".");
		if(dotpos == std::string::npos)
			dotpos = file.size();
		std::string name = file.substr(dashpos, dotpos - dashpos);
		return name;
	}

	std::string GetFileExtension(std::string file) {
		size_t dotpos = file.find_last_of(".");
		if(dotpos == std::string::npos)
			return "";
		std::string extension = file.substr(dotpos + 1);
		std::transform(extension.begin(), extension.end(), extension.begin(), ::tolower);
		return extension;
	}

private:
    std::string music_name;
    void maintain();
    const std::unique_ptr<OpenALSingleton>& openal;
    std::unordered_map<std::string, std::shared_ptr<OpenALSoundBuffer>> buffers;
    std::unordered_set<ALuint> playing;
    float volume = 1.0f;
};

}

#endif //YGOPEN_SOUND_OPENAL_H
