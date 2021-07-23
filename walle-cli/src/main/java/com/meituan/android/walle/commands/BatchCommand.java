package com.meituan.android.walle.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.meituan.android.walle.ChannelWriter;
import com.meituan.android.walle.SignatureNotFoundException;
import com.meituan.android.walle.utils.CommaSeparatedKeyValueConverter;
import com.meituan.android.walle.utils.Util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parameters(commandDescription = "channel apk batch production")
public class BatchCommand implements IWalleCommand {

    @Parameter(required = true, description = "inputFile [outputDirectory]", arity = 2, converter = FileConverter.class)
    private List<File> files;

    @Parameter(names = {"-e", "--extraInfo"}, converter = CommaSeparatedKeyValueConverter.class, description = "Comma-separated list of key=value info, eg: -e time=1,type=android")
    private Map<String, String> extraInfo;

    @Parameter(names = {"-c", "--channelList"}, description = "Comma-separated list of channel, eg: -c meituan,xiaomi")
    private List<String> channelList;

    @Parameter(names = {"-f", "--channelFile"}, description = "channel file")
    private File channelFile;

    @Override
    public void parse() {
        final File inputFile = files.get(0);
        File outputDir = null;
        if (files.size() == 2) {
            outputDir = Util.removeDirInvalidChar(files.get(1));
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
        } else {
            outputDir = inputFile.getParentFile();
        }

        List<String> lines = new ArrayList<>();
        if (channelList != null) {
            lines.addAll(channelList);
        }

        if (channelFile != null) {
            try {
                lines.addAll(IOUtils.readLines(new FileInputStream(channelFile), "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (lines != null) {
            for (String line : lines) {
                final String lineTrim = line.trim();
                if (lineTrim.length() == 0 || lineTrim.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("#");
                final String channel = split[0].trim();
                final String alias = split.length > 1 ? split[1].trim() : null;
                if (channel.length() != 0) {
                    generateChannelApk(inputFile, outputDir, channel, alias);
                }
            }
        }
    }

    private void generateChannelApk(final File inputFile, final File outputDir, final String channel, final String alias) {
        final String name = FilenameUtils.getBaseName(inputFile.getName());
        final String extension = FilenameUtils.getExtension(inputFile.getName());
        final String newName = name + (alias == null || alias.isEmpty() ? "" : "_" + alias) + "_" + channel + "." + extension;
        final File channelApk = new File(outputDir, newName);
        try {
            FileUtils.copyFile(inputFile, channelApk);
            ChannelWriter.put(channelApk, channel, extraInfo);
        } catch (IOException | SignatureNotFoundException e) {
            e.printStackTrace();
        }
    }
}
