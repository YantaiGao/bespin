package io.bespin.java.mapreduce.bfs;

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

/**
 * Tool for extracting nodes that are reachable from the source node.
 *
 * @author Jimmy Lin
 */
public class FindReachableNodes extends Configured implements Tool {
  private static final Logger LOG = Logger.getLogger(FindReachableNodes.class);

  private static class MyMapper extends Mapper<IntWritable, BfsNode, IntWritable, BfsNode> {
    @Override
    public void map(IntWritable nid, BfsNode node, Context context)
        throws IOException, InterruptedException {
      if (node.getDistance() < Integer.MAX_VALUE) {
        context.write(nid, node);
      }
    }
  }

  public FindReachableNodes() {}

  public static class Args {
    @Option(name = "-input", metaVar = "[path]", required = true, usage = "input path")
    public String input;

    @Option(name = "-output", metaVar = "[path]", required = true, usage = "output path")
    public String output;
  }

  /**
   * Runs this tool.
   */
  public int run(String[] argv) throws Exception {
    Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(100));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return -1;
    }

    LOG.info("Tool name: " + this.getClass().getName());
    LOG.info(" - inputDir: " + args.input);
    LOG.info(" - outputDir: " + args.output);

    Job job = Job.getInstance(getConf());
    job.setJobName(String.format("FindReachableNodes:[input: %s, output: %s]",
        args.input, args.output));
    job.setJarByClass(FindReachableNodes.class);

    job.setNumReduceTasks(0);

    job.getConfiguration().setInt("mapred.min.split.size", 1024 * 1024 * 1024);

    FileInputFormat.addInputPath(job, new Path(args.input));
    FileOutputFormat.setOutputPath(job, new Path(args.output));

    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    job.setMapOutputKeyClass(IntWritable.class);
    job.setMapOutputValueClass(BfsNode.class);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(BfsNode.class);

    job.setMapperClass(MyMapper.class);

    // Delete the output directory if it exists already.
    FileSystem.get(job.getConfiguration()).delete(new Path(args.output), true);

    job.waitForCompletion(true);

    return 0;
  }

  /**
   * Dispatches command-line arguments to the tool via the <code>ToolRunner</code>.
   */
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new FindReachableNodes(), args);
    System.exit(res);
  }
}
